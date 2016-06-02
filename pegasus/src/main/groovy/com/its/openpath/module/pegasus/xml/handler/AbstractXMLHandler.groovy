package com.its.openpath.module.pegasus.xml.handler

import javax.xml.parsers.DocumentBuilderFactory

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.xml.sax.InputSource

import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode
import com.its.openpath.module.opscommon.model.messaging.ops.RoomAvailabilitySearchType
import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.opscommon.util.PersistenceMessageBusQueueNames
import com.its.openpath.module.opscommon.util.TimeUUIDUtils
import com.its.openpath.module.pegasus.AbstractBaseHandler
import com.its.openpath.module.pegasus.StatisticsDashboard

/**
 * <code>AbstractXMLHandler</code>
 * <p/>
 * Base class of all OTA XML Service Request Handler classes. Contains methods common to all subclasses and the main {@link AbstractXMLHandler#execute}
 * method which acts as the main entry point and drives the workflow.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

abstract class AbstractXMLHandler
extends AbstractBaseHandler
{
  private static final Logger sLogger = LoggerFactory.getLogger( AbstractXMLHandler.class.name )


  /**
   * This is called by the {@link com.its.openpath.module.pegasus.RequestProcessorServlet} to process the
   * XML message received from Pegasus USW
   * <p />
   * NOTE: Subclass handlers must ensure catching and handling all exceptions and creating an error response as appropriate.
   * No Exceptions must be propergated to this level. Any uncaught exceptions at this level will result in an error
   * response sent back to USW.
   * <p />
   * @param messageType - The type of message
   * @param request - Incoming XML stream
   * @return String - XML stream to be sent back to USW
   * @see {@link AbstractBaseHandler#execute}
   * @throws IllegalStateException - If the Service Request received cannot be parsed
   */
  def String execute( Object messageType, String requestXML )
  throws IllegalStateException
  {
    RoomAvailabilitySearchType roomAvailabilityMsgType = (RoomAvailabilitySearchType) messageType
    def requestMessageXML

    sLogger.debug "*** PEGASUS - Procesing a new: [${roomAvailabilityMsgType}] Service Request from Pegasus USW ..."
    sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
    if ( sLogger.isDebugEnabled() )
    {
      sLogger.debug "*** PEGASUS - The new Service Request received is:\n [$requestXML]"
    }

    // Create the OpenPath Context container and set it as a ThreadLocal so that it's available downstream
    UUID uuid = TimeUUIDUtils.getUniqueTimeUUIDinMicros()
    InvocationContext context = InvocationContext.getNewInstance( uuid )
    context.addSessionDataItem( "MESSAGE_TYPE", messageType )

    /**
     * PUBLISH REQUEST EVENT TO MESSAGE BUS
     */
    publishEventToMessageBus( uuid, PersistenceMessageBusQueueNames.REQUEST_XML_QUEUE, messageType.number, RoomAvailabilitySearchType.class.name, "pegasus.endpoint", "reztrip.endpoint", requestXML )

    // Parse the new request and set in the ThreadLocal InvocationContext so it can be accessed downstream
    try
    {
      requestMessageXML = new XmlSlurper().parseText( requestXML ).
        declareNamespace(
          'SOAP-ENV': 'http://schemas.xmlsoap.org/soap/envelope/' )
      // Subclass Handlers and Builders will access the Service Request Data from the InvocationContext
      context.externalSystemRequestData = requestMessageXML

      // Save some helpful contextual info that can be logged
      def extRefId = requestMessageXML.Body.OTA_HotelAvailRQ.@EchoToken
      def TXN_REF = "CorRelId: [${context.correlationId}], ExtSysId: [${extRefId}], Type: [${context.getSessionDataItem( "MESSAGE_TYPE" )}]"
      context.addSessionDataItem( "TXN_REF", TXN_REF )
      StatisticsDashboard.lastServiceRequestInfo.set( TXN_REF )

      // Save the parsed Service Request as a org.w3c.dom.Document, it will be used to build the Service Response
      def builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
      Document document = builder.parse( new InputSource( new StringReader( requestXML ) ) )
      context.addSessionDataItem( "REQ_DOC", document )
    }
    catch ( Throwable e )
    {
      StatisticsDashboard.unrecognizedRequestsReceivedCount.andIncrement
      def errorMsg = "PEGASUS - FATAL ERRROR - Couldn't parse the: [${roomAvailabilityMsgType}] Service Request received from Pegasus USW"
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
      def errMsg = "PEGASUS - FATAL ERRROR - Couldn't process a: [${roomAvailabilityMsgType}] request, caught an unhandled exception thrown by a subclass during request validation"
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error errMsg
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error "Exception caught: ${e.message}", e
      mMessageBuilder.buildErrorResponse( OpsErrorCode.SERVICE_REQUEST_PROCESSING_ERROR, errMsg )
    }

    // Process the received XML request using the subclass
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
          sLogger.debug "***PEGASUS -  Service Request failed validation: [${context.getSessionDataItem( "TXN_REF" )}]"
        }
      }
    }
    catch ( Throwable e )
    {
      def errMsg = "PEGASUS - FATAL ERRROR - Couldn't process a: [${roomAvailabilityMsgType}] request, caught an unhandled exception thrown by a subclass during request processing"
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error errMsg
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error "Exception caught: ${e.message}", e
      mMessageBuilder.buildErrorResponse( OpsErrorCode.SERVICE_REQUEST_PROCESSING_ERROR, errMsg )
    }
    finally
    {
      /**
       * AND FINALLY, PUBLISH RESPONSE EVENT TO MESSAGE BUS, WHETHER AN ERROR OCCURRED OR NOT
       */
      publishEventToMessageBus( uuid, PersistenceMessageBusQueueNames.RESPONSE_XML_QUEUE, messageType.number, RoomAvailabilitySearchType.class.name, "pegasus.endpoint", "reztrip.endpoint", context.externalSystemResponseData )
    }

    return context.externalSystemResponseData
  }
}