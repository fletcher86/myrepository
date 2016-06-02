package com.its.openpath.module.ratetiger

import com.dyuproject.protostuff.JsonIOUtil
import com.its.openpath.module.opscommon.comm.bus.IMessageBus
import com.its.openpath.module.opscommon.comm.bus.IMessageBusSubscriber
import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.model.messaging.ops.RateManagementRequest
import com.its.openpath.module.opscommon.util.InvocationContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

/**
 * <code>RateManagementNotifSubscriber</code>
 * <p/>
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service("RateManagementNotifSubscriber")
@ManagedResource('OPENPATH:name=/module/ratetiger/RateManagementNotifSubscriber')
class RateManagementNotifSubscriber
implements IMessageBusSubscriber
{
  private static final Logger sLogger = LoggerFactory.getLogger( RateManagementNotifSubscriber.class.name )

  @Autowired(required = true)
  protected IMessageBus mOpsMessageBus


  /**
   * Constructor
   */
  RateManagementNotifSubscriber( )
  {
    sLogger.info 'Instantiated ...'
  }


  /**
   * Called by the Spring Framework after all Spring-manages beans are instantiated and wired up.
   */
  @PostConstruct
  void init( )
  {
    mOpsMessageBus.consumeFromQueue( "RATE_MANAGEMENT_NOTIF", this )
  }

  /**
   * @see {@link com.its.openpath.module.opscommon.comm.bus.IMessageBusSubscriber#getSubscriberId}
   */
  String getSubscriberId( )
  {
    return this.class.name
  }

  /**
   * @see {@link com.its.openpath.module.opscommon.comm.bus.IMessageBusSubscriber#onMessage}
   */
  void onMessage( final String s, final OpsMessage opsMessage )
  {
    def notifJSON = opsMessage.data
    RateManagementRequest notifObj = new RateManagementRequest()
    InvocationContext context

    sLogger.debug "*** Rcvd a new Rate Management Notification from OPS Bus ..."
    sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
    if ( sLogger.isDebugEnabled() )
    {
      sLogger.debug "*** The new Service Request received is:\n [$notifJSON]"
    }

    // Deserialize the JSON stream to its object form
    try
    {
      JsonIOUtil.mergeFrom( notifJSON.bytes, notifObj, RateManagementRequest.schema, false )
      // Create the OpenPath Context container and set it as a ThreadLocal so that it's available downstream
      context = InvocationContext.getNewInstance( opsMessage.correlationId )
      def TXN_REF = "CorRelId: [${context.correlationId}]"
      context.addSessionDataItem( "TXN_REF", TXN_REF )
    }
    catch ( Throwable e )
    {
      logAndHandleError( notifObj, "Couldn't parse the OPS JSON request received; ", e )
    }

    //
    //    responseJSON = super.postAvlRequestToRezTripCRS( notifJSON, notifObj, handleResponseFromRezTrip ) { String endpointURL, int httpStatusCode, Throwable exception ->
    //      def errorMessage = "Couldn't POST Service Request: [${context.getSessionDataItem( "TXN_REF" )}] to RezTrip REST Endpoint: [${endpointURL}]"
    //      sLogger.error "${errorMessage}"
    //      sLogger.error "*************************************************************************************************"
    //      if ( !exception )
    //      {
    //        sLogger.error "*** HTTP Error Code Rcvd: ${httpStatusCode}"
    //        sLogger.error "*** Request to be sent: ${notifJSON}"
    //      }
    //
    //      OpsErrorCode errorCode = OpsErrorCode.SERVICE_RESPONSE_PROCESSING_ERROR
    //      switch ( httpStatusCode )
    //      {
    //        case 500:
    //          errorCode = OpsErrorCode.SERVICE_RESPONSE_ENDPOINT_UNAVAILABLE
    //          break
    //      }
    //
    //      return super.logAndBuildErrorResponse( (AvailabilityRequest) notifObj,
    //        errorMessage, exception, errorCode )
    //    }
  }

  /**
   * Closure block invoked to process the response received from the CRS, if needed to.
   */
  Closure handleResponseFromRezTrip = {  String responseJSON ->

    // Not doing anything with the response yet
    return responseJSON
  }

  /**
   * Helper method to handle and error that occurred during POSTing to OPS REST Endpoint.
   * <p />
   * @param requestObj - request in Object form
   * @param errorMessage - Descriptive error message
   * @param e - Exception caught
   */
  def logAndHandleError( RateManagementRequest requestObj, String errorMessage, Throwable e )
  {
    sLogger.error "*************************************************************************************************"
    sLogger.error "${errorMessage}"
    sLogger.error "*************************************************************************************************"
    sLogger.error errorMessage, e
  }

}
