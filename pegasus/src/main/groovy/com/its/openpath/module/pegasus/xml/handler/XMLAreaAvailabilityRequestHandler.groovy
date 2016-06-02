package com.its.openpath.module.pegasus.xml.handler

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service
import com.its.openpath.module.pegasus.xml.builder.XMLAreaAvailabilityMessageBuilder

import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.model.messaging.ops.RoomAvailabilitySearchType
import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.opscommon.util.TimeUUIDUtils
import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode
import com.its.openpath.module.pegasus.StatisticsDashboard

/**
 * <code>XMLAreaAvailabilityRequestHandler</code>
 * <p/>
 * Handles Area Availability Service Requests from Pegasus USW.
 * <p />
 * An OTA OTA_HotelAvailRQ messages is expected by this Handler. Performs basic validation of the received message,
 * creates an OPS JSON request, and invokes the OPS Service Broker to process the request.
 * <p />
 * An OTA_HotelAvailRS XML message is created to be sent to Pegasus USW using the the OPS JSON response received from the
 * broker.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service
@ManagedResource('OPENPATH:name=/module/pegasus/xml/XMLAreaAvailabilityRequestHandler')
class XMLAreaAvailabilityRequestHandler
extends AbstractXMLHandler
{
  private static final Logger sLogger = LoggerFactory.getLogger( XMLAreaAvailabilityRequestHandler.class.name )

  @Autowired(required = true)
  protected XMLAreaAvailabilityMessageBuilder mMessageBuilder


  /**
   * Constructor
   */
  XMLAreaAvailabilityRequestHandler( )
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
    StatisticsDashboard.areaAvailabilityRequestCount.andIncrement

    // Build the OpenPath JSON Request to be sent to the Availability Request Service Provider
    if ( !mMessageBuilder.buildRequestToOpenPathFromExternal() )
    {
      // If unable to build the JSON Req, the Builder is expected to create an Error Response and set in the Context
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
    opsReqMessage.messageType = RoomAvailabilitySearchType.AREA_AVAILABILITY.number
    opsReqMessage.correlationId = context.correlationId.toString ()
    opsReqMessage.correlationIdBytes = context.correlationIdByteString
    opsReqMessage.timestamp = TimeUUIDUtils.getTimeFromUUID(context.correlationId)
    opsReqMessage.data = requestJSON

    boolean success = mOpsMessageBus.queueMessage( "AVAILABILITY_REQ", context.correlationId.toString (), 5000, opsReqMessage ) {
      OpsMessage opsRspMessage ->
      context.openPathResponseData = opsRspMessage.data
      if ( sLogger.isDebugEnabled() )
      {
        sLogger.debug( "*** PEGASUS - Rcvd from OpsBus: [${context.getSessionDataItem( "TXN_REF" )}]" )
        sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
        sLogger.debug "${opsRspMessage.data}"
      }
    }
    if ( !success )
    {
      def errMsg = "PEGASUS - Timed out receiving a responese from OPS Bus for [${context.getSessionDataItem( "TXN_REF" )}]"
      sLogger.error errMsg
      mMessageBuilder.buildErrorResponse( OpsErrorCode.SERVICE_PROVIDER_UNAVAILABLE, errMsg )
      return
    }

    // Build the XML OTA response to be sent to Pegasus USW
    mMessageBuilder.buildResponseToExternalFromOpenPath()
    def responseXML = context.externalSystemResponseData
    if ( sLogger.isDebugEnabled() )
    {
      sLogger.debug( "*** PEGASUS - OTA Xml Rsp Msg to USW for: [${context.getSessionDataItem( "TXN_REF" )}] is:" )
      sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.debug "[${responseXML}]"
    }
  }

}
