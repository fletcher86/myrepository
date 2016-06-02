package com.its.openpath.module.pegasus.amf.handler

import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode
import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.opscommon.util.PersistenceMessageBusQueueNames
import com.its.openpath.module.opscommon.util.TimeUUIDUtils
import com.its.openpath.module.pegasus.AbstractBaseHandler
import com.its.openpath.module.pegasus.StatisticsDashboard
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * <code>AbstractAMFHandler</code>
 * <p/>
 * Base class of all AMF Service Request Handler classes. Contains methods common to all subclasses and the main {@link AbstractBaseHandler#execute}
 * method which acts as the main entry point and drives the workflow.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

abstract class AbstractAMFHandler
extends AbstractBaseHandler
{
  private static final Logger sLogger = LoggerFactory.getLogger( AbstractAMFHandler.class.name )


  /**
   * This is called by the {@link com.its.openpath.module.pegasus.RequestProcessorServlet} to process the
   * Service Request message received from the Pegasus USW.
   * <p />
   * NOTE: Subclass handlers must ensure catching and handling all exceptions and creating an error response as appropriate.
   * No Exceptions must be propergated to this level. Any uncaught exceptions at this level will result in an error
   * response sent back to USW.
   * <p />
   * @param messageType - The type of message
   * @param request - Service Request received
   * @return String - Response to be sent back to USW
   * @see {@link AbstractBaseHandler#execute}
   * @throws IllegalStateException - If the Service Request received cannot be parsed
   */
  protected String execute( final Object messageType, final String requestMessage )
  throws IllegalStateException
  {
    def requestMsgType = messageType
    sLogger.debug "*** PEGASUS - Procesing a new: [${requestMsgType}] Service Request from Pegasus USW ..."
    sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
    if ( sLogger.isDebugEnabled() )
    {
      sLogger.debug "*** PEGASUS - The new Service Request received is:\n [$requestMessage]"
    }

    // Create the OpenPath Context container and set it as a ThreadLocal so that it's available downstream
    UUID uuid = TimeUUIDUtils.getUniqueTimeUUIDinMicros()
    InvocationContext context = InvocationContext.getNewInstance( uuid )
    context.addSessionDataItem( "MESSAGE_TYPE", messageType )

    /**
     * SEND REQUEST EVENT TO MESSAGE BUS TO BE STORED IN DATABASE HERE
     */
    OpsMessage reqMsg = new OpsMessage()
    reqMsg.messageType = messageType.number
    reqMsg.source = "pegasus.endpoint"
    reqMsg.destination = "reztrip.endpoint"
    reqMsg.correlationId = uuid.toString()
    reqMsg.correlationIdBytes = TimeUUIDUtils.asByteString( uuid )
    reqMsg.timestamp = TimeUUIDUtils.getTimeFromUUID( uuid )
    reqMsg.data = requestMessage
    mOpsMessageBus.queueMessage( PersistenceMessageBusQueueNames.REQUEST_XML_QUEUE, reqMsg )

    // Parse the new request and set in the ThreadLocal InvocationContext so it can be accessed downstream
    try
    {
      String header = requestMessage.substring( 0, requestMessage.indexOf( '||' ) )
      header.normalize()
      // Save the header, it needs to be returned intact, in the the response
      context.addSessionDataItem( "MESSAGE_HDR", header )
      Map<String, String> headerValueMap = new LinkedHashMap<String, String>()
      def headerElements = header.split( "\\|" )
      headerElements.each { String element ->
        if ( element.contains( 'HDR' ) )
        {
          // Ignore the Header Segment name
          return
        }
        // Element name is the first 3 chars, the rest is the data -- GMT051756
        headerValueMap.put( element.substring( 0, 3 ), element.substring( 3, element.length() ) )
      }
      if ( sLogger.debugEnabled )
      {
        sLogger.debug "PEGASUS - AMF Message Header rcvd: ${headerValueMap}"
      }

      // Subclass Handlers and Builders will access the Service Request Data from the InvocationContext
      context.externalSystemRequestData = requestMessage

      // Save some helpful contextual info that can be logged -- HRS and IAT elements are always present
      def extRefId = "HRS:${headerValueMap.get( 'HRS' )}, IAT:${headerValueMap.get( 'IAT' )}, MSN: ${headerValueMap.get( 'MSN' )}"
      def TXN_REF = "CorRelId: [${context.correlationId}], ExtSysRef: [${extRefId}], Type: [${context.getSessionDataItem( "MESSAGE_TYPE" )}]"
      context.addSessionDataItem( "TXN_REF", TXN_REF )
      StatisticsDashboard.lastServiceRequestInfo.set( TXN_REF )

      // Save the parsed Header Map it will be used to build the Header in the Service Response
      context.addSessionDataItem( "REQ_HEADER", headerValueMap )
    }
    catch ( Throwable e )
    {
      StatisticsDashboard.unrecognizedRequestsReceivedCount.andIncrement
      def errorMsg = "PEGASUS - FATAL ERRROR - Couldn't parse the: [${requestMsgType}] Service Request received from Pegasus USW"
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error errorMsg
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error "Exception caught: ${e.message}", e
      // Servlet will catch this and send a SOAP Fault
      throw new IllegalStateException( errorMsg )
    }

    // Validate the received XML request
    def validMessage = false
    try
    {
      validMessage = this.validate()
    }
    catch ( Throwable e )
    {
      def errMsg = "PEGASUS - FATAL ERRROR - Couldn't process a: [${requestMsgType}] request, caught an unhandled exception thrown by a subclass during request validation"
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error errMsg
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error "Exception caught: ${e.message}", e
      mMessageBuilder.buildErrorResponse( OpsErrorCode.SERVICE_PROVIDER_UNAVAILABLE, errMsg )
    }

    // Delegate the processing of the received AMF message to a subclass
    try
    {
      if ( validMessage )
      {
        this.process()
      }
      else
      {
        if ( sLogger.isDebugEnabled() )
        {
          sLogger.debug "*** PEGASUS -  Service Request failed validation: [${context.getSessionDataItem( "TXN_REF" )}]"
        }
      }
    }
    catch ( Throwable e )
    {
      def errMsg = "PEGASUS - FATAL ERRROR - Couldn't process a: [${requestMsgType}] request, caught an unhandled exception thrown by a subclass during request processing"
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error errMsg
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error "Exception caught: ${e.message}", e
      mMessageBuilder.buildErrorResponse( OpsErrorCode.SERVICE_PROVIDER_UNAVAILABLE, errMsg )
    }
    finally
    {
      /**
       * AND FINALLY, SEND RESPONSE EVENT TO MESSAGE BUS TO BE STORED IN DATABASE HERE, WHETHER ERROR OR NO ERROR
       */
      OpsMessage rspMsg = new OpsMessage()
      rspMsg.messageType = messageType.number
      rspMsg.source = "pegasus.endpoint"
      rspMsg.destination = "reztrip.endpoint"
      rspMsg.correlationId = uuid.toString()
      rspMsg.correlationIdBytes = TimeUUIDUtils.asByteString( uuid )
      rspMsg.timestamp = TimeUUIDUtils.getTimeFromUUID( uuid )
      rspMsg.data = context.externalSystemResponseData
      mOpsMessageBus.queueMessage( PersistenceMessageBusQueueNames.RESPONSE_XML_QUEUE, rspMsg )
    }

    return context.externalSystemResponseData
  }

}