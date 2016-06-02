package com.its.openpath.module.pegasus.amf.handler

import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ReservationManagementType
import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.pegasus.amf.builder.AMFReservationManagementMessageBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service
import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode
import com.its.openpath.module.pegasus.StatisticsDashboard

/**
 * <code>AMFReservationManagementHandler</code>
 * <p/>
 * Handles Reservation Management (Create/Update/Delete/Ignore/End) Service Requests from Pegasus USW.
 * <p />
 * Performs basic validation of the received message,
 * creates an OPS JSON request, and invokes the OPS Service Broker to process the request.
 * <p />
 * The response received from the Service Broker is converted to an AMF message and sent back to Pegasus USW.
 * broker.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service
@ManagedResource('OPENPATH:name=/module/pegasus/amf/AMFReservationManagementHandler')
class AMFReservationManagementHandler
extends AbstractAMFHandler
{
  private static final Logger sLogger = LoggerFactory.getLogger( AMFReservationManagementHandler.class.name )

  @Autowired(required = true)
  protected AMFReservationManagementMessageBuilder mMessageBuilder


  /**
   * Constructor
   */
  AMFReservationManagementHandler( )
  {
    sLogger.info "Instantiated ..."
  }

  /**
   * @see {@link com.its.openpath.module.pegasus.AbstractBaseHandler#validate}
   */
  @Override
  def boolean validate( )
  {
    //mAvailabilityRequestBuilder.buildErrorResponse( "3", "136", "EN", "FATAL ERROR" )
    return true
  }

  /**
   * @see {@link com.its.openpath.module.pegasus.AbstractBaseHandler#process}
   */
  @Override
  def void process( )
  {
    InvocationContext context = InvocationContext.instance
    ReservationManagementType reservationMsgType = (ReservationManagementType) context.getSessionDataItem( "MESSAGE_TYPE" )

    switch ( reservationMsgType )
    {
      case ReservationManagementType.NEW_RESERVATION:
        StatisticsDashboard.newReservationRequestCount.andIncrement
        break
      case ReservationManagementType.CANCEL_RESERVATION:
        StatisticsDashboard.cancelReservationRequestCount.andIncrement
        break
      case ReservationManagementType.IGNORE_RESERVATION:
        StatisticsDashboard.ignoreReservationRequestCount.andIncrement
        break
      case ReservationManagementType.END_RESERVATION:
        StatisticsDashboard.endTransactionRequestCount.andIncrement
        break
      case ReservationManagementType.MODIFY_RESERVATION:
        StatisticsDashboard.modifyReservationRequestCount.andIncrement
    }

    // Build the OpenPath JSON Request to be sent to the Reservation Management Service Provider
    if ( !mMessageBuilder.buildRequestToOpenPathFromExternal() )
    {
      // If unable to build the JSON Req, the Builder is expected to create an Error Response and set in the InvocationContext
      return
    }

    // Route to the Service Provider via the Service Broker and get the response
    if ( sLogger.isDebugEnabled() )
    {
      sLogger.debug( "*** PEGASUS - Publish to OpsBus: [${context.getSessionDataItem( "TXN_REF" )}]" )
      sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
    }
    def requestJSON = context.openPathRequestData
    OpsMessage opsReqMessage = new OpsMessage()
    opsReqMessage.messageType = reservationMsgType.number
    opsReqMessage.correlationId = context.correlationId
    opsReqMessage.correlationIdBytes = context.correlationIdByteString
    opsReqMessage.timestamp = new Date().time
    opsReqMessage.data = requestJSON

    switch ( reservationMsgType )
    {
      case ReservationManagementType.NEW_RESERVATION:
      case ReservationManagementType.CANCEL_RESERVATION:
      case ReservationManagementType.IGNORE_RESERVATION:
      case ReservationManagementType.END_RESERVATION:
        boolean success = mOpsMessageBus.queueMessage( "RESERVATION_REQ", context.correlationId.toString(), 10000, opsReqMessage ) {
          OpsMessage opsRspMessage ->
          context.openPathResponseData = opsRspMessage.data
          if ( sLogger.isDebugEnabled() )
          {
            sLogger.debug( "*** PEGASUS - Rcvd from OpsBus: [${context.getSessionDataItem( "TXN_REF" )}]" )
            sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
          }
        }
        if ( !success )
        {
          def errMsg = "PEGASUS - Timed out receiving a responese from OPS Bus for [${context.getSessionDataItem( "TXN_REF" )}]"
          sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
          sLogger.error errMsg
          sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
          mMessageBuilder.buildErrorResponse( OpsErrorCode.SERVICE_PROVIDER_UNAVAILABLE, errMsg )
        }

        // Build the XML OTA response to be sent to Pegasus USW
        mMessageBuilder.buildResponseToExternalFromOpenPath()
        def responseXML = context.externalSystemResponseData
        if ( sLogger.isDebugEnabled() )
        {
          sLogger.debug( "*** PEGASUS - AMF Msg Rsp to USW for: [${context.getSessionDataItem( "TXN_REF" )}] is:" )
          sLogger.debug "[${responseXML}]"
        }
        break

      case ReservationManagementType.MODIFY_RESERVATION:
        // Modify Reservation Requests are one way, no Response to be sent back to USW
        mOpsMessageBus.queueMessage( 'RESERVATION_REQ', opsReqMessage )
        context.externalSystemResponseData = "NO_RESPONSE_TO_SEND"
        break
    }
  }

}
