package com.its.openpath.module.siteminder.subscribers

import groovy.util.slurpersupport.GPathResult

import java.util.concurrent.atomic.AtomicLong

import javax.ws.rs.core.Response

import org.apache.cxf.helpers.IOUtils
import org.apache.cxf.jaxrs.client.WebClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jmx.export.annotation.ManagedAttribute

import com.dyuproject.protostuff.JsonIOUtil
import com.dyuproject.protostuff.Message
import com.its.openpath.module.opscommon.comm.bus.IMessageBus
import com.its.openpath.module.opscommon.comm.bus.IMessageBusSubscriber
import com.its.openpath.module.opscommon.model.messaging.ops.AvailabilityRequest
import com.its.openpath.module.opscommon.model.messaging.ops.AvailabilityRequestType
import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.model.messaging.ops.OpsTxnType
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateRequest
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.NotificationReportRequest
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.ReservationRequest
import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.opscommon.util.PersistenceMessageBusQueueNames
import com.its.openpath.module.opscommon.util.OpsStatuses
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

/**
 * <code>AbstractSiteminderBaseSubscriber</code>
 * <p/>
 * Base subscriber class
 * <p />
 * @author Kent Fletcher
 * @since June 2012
 */

abstract class AbstractSiteminderBaseSubscriber
implements IMessageBusSubscriber
{
  @Autowired(required = true)
  protected IMessageBus mOpsMessageBus
  
  
  private static final Logger sLogger = LoggerFactory.getLogger( AbstractSiteminderBaseSubscriber.class.name )
  
  private @Value("#{runtimeProperties['siteminder.base.url']}")
  String mSiteminderBaseURL
  
  // The URL or POST header values doesn't change per-request, so using an instance like this is thread safe
  private WebClient mSiteminderWSClient = WebClient.create( "${mSiteminderBaseURL}" )
  
  protected AtomicLong mTotalReceivedCount = new AtomicLong( 0 )
  protected AtomicLong mSuccessfulRequestCount = new AtomicLong( 0 )
  protected AtomicLong mFailedRequestCount = new AtomicLong( 0 )
  protected AtomicLong mValidationFailedCount = new AtomicLong( 0 )
  protected AtomicLong mSuccessfulResponseCount = new AtomicLong( 0 )
  protected AtomicLong mErrorResponseCount = new AtomicLong( 0 )
  
  /**
   * DEFINE A BUNCH OF X'S FOR LOGGING PURPOSES FOR ERRORS
   */
  public static final String A_BUNCH_OF_XS =  "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
  
  /**
   * Return the running count of all received Service Requests.
   * <p />
   * @return long - The count
   */
  @ManagedAttribute
  def long getTotalReceivedCount( )
  {
    return mTotalReceivedCount.get()
  }
  
  /**
   * Return the running count of successfully processed Service Requests.
   * <p />
   * @return long - The count
   */
  @ManagedAttribute
  def long getSuccessfulRequestCount( )
  {
    return mSuccessfulRequestCount.get()
  }
  
  /**
   * Return the running count of failed Service Requests.
   * <p />
   * @return long - The count
   */
  @ManagedAttribute
  def long getFailedRequestCount( )
  {
    return mFailedRequestCount.get()
  }
  
  /**
   * Return the running count of Service Requests that failed validation.
   * <p />
   * @return long - The count
   */
  @ManagedAttribute
  def long getValidationFailedCount( )
  {
    return mValidationFailedCount.get()
  }
  
  /**
   * Return the running count of successfully processed Service Responses.
   * <p />
   * @return long - The count
   */
  @ManagedAttribute
  def long getSuccessfulResponseCount( )
  {
    return mSuccessfulResponseCount.get()
  }
  
  /**
   * Return the running count of responses that were sent with 'Error' status.
   * <p />
   * @return long - The count
   */
  @ManagedAttribute
  def long getErrorResponseCount( )
  {
    return mErrorResponseCount.get()
  }
  
  /**
   * Reset all counters.
   * <p />
   * @return long - The count
   */
  @ManagedAttribute
  def void resetCounts( )
  {
    mSuccessfulRequestCount.set( 0 )
    mFailedRequestCount.set( 0 )
    mSuccessfulResponseCount.set( 0 )
    mTotalReceivedCount.set( 0 )
    mValidationFailedCount.set( 0 )
    mErrorResponseCount.set( 0 )
  }
  
  
  /**
   * Publish event OpsMessage to message bus
   *<p />
   *@param uuid UUID, time base uuid generated by TimeUUIDUtils
   *@param queueName String, message bus queue name
   *@param messageType Integer, messageType from enum
   *@param source String, source of the event
   *@param destination String, destination of the event
   *@param data String, the payload for this event
   */
  def void publishEventToMessageBus(UUID uuid, String queueName, Integer messageType, String messageSubType, String source, String destination, String data)
  {
    OpsMessage msg = new OpsMessage()
    msg.messageType =messageType
    msg.messageSubType = messageSubType
    msg.source = source
    msg.destination = destination
    msg.correlationId = uuid.toString()
    msg.correlationIdBytes = TimeUUIDUtils.asByteString( uuid )
    msg.timestamp = TimeUUIDUtils.getTimeFromUUID( uuid )
    msg.data = data
    mOpsMessageBus.queueMessage(queueName, msg)
  }
  
  
  /**
   * Process opsMessage, convert to soap xml request and send to siteminder rest endpoint
   *<p />
   *@param opsMessage OpsMessage
   *@param type Enum
   *@throws RuntimeException
   */
  protected void processMessage(OpsMessage opsMessage, def type)
  throws RuntimeException
  {
    String source = "reztrip.endpoint"
    String destination = "siteminder.endpoint"
    
    // get correlationId
    UUID correlationId = TimeUUIDUtils.toUUID( opsMessage.getCorrelationIdBytes ().toByteArray () )
    InvocationContext context = InvocationContext.getNewInstance( correlationId )
    def TXN_REF = "CorRelId: [${context.correlationId}]"
    context.addSessionDataItem( "TXN_REF", TXN_REF )
    
    def notifJSON = opsMessage.data
    String responseXML
    
    if ( sLogger.isDebugEnabled() )
    {
      sLogger.debug "*** SITEMINDER - Rcvd a new ${type} Notification from OPS Bus ..."
      sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.debug "*** SITEMINDER - The new Service Request received is:\n [$notifJSON]"
    }
    
    Message notifObj
    Integer messageType
    try
    {
      switch (type)
      {
        case OpsTxnType.AVAILABILITY_MGMT:
          notifObj = new AvailabilityRequest()
          messageType=  AvailabilityRequestType.HOTEL_AVAILABILITY_NOTIF.number
          JsonIOUtil.mergeFrom( notifJSON.bytes, notifObj, notifObj.schema, false )
          break
        case OpsTxnType.RATE_MGMT:
          notifObj = new RateRequest()
          JsonIOUtil.mergeFrom( notifJSON.bytes, notifObj, notifObj.schema, false )
          break
        case OpsTxnType.RESERVATION_MGMT:
          notifObj = new ReservationRequest()
          JsonIOUtil.mergeFrom( notifJSON.bytes, notifObj, notifObj.schema, false )
          break
        case OpsTxnType.RESERVATION_NOTIFICATION_REPORT_ERRORS_MGMT:
          notifObj = new NotificationReportRequest()
          JsonIOUtil.mergeFrom( notifJSON.bytes, notifObj, notifObj.schema, false )
          correlationId = TimeUUIDUtils.toUUID ( ((NotificationReportRequest)notifObj).echoTokenBytes.getBytes() )
          context = InvocationContext.getNewInstance( correlationId )
          break
        case OpsTxnType.RESERVATION_NOTIFICATION_REPORT_SUCCESS_MGMT:
          notifObj = new NotificationReportRequest()
          JsonIOUtil.mergeFrom( notifJSON.bytes, notifObj, notifObj.schema, false )
          correlationId = TimeUUIDUtils.toUUID ( ((NotificationReportRequest)notifObj).echoTokenBytes.getBytes() )
          context = InvocationContext.getNewInstance( correlationId )
          break
      }
    }
    catch ( Throwable e )
    {
      logAndHandleError( notifObj, "SITEMINDER - Couldn't parse the OPS JSON Hotel Availability request received; ", e )
    }
    
    /**
     * this.mSoapXMLbuilder is in the enclosing concrete instance calling its implementation of buildSoapXMLMessage
     */
    String notifXML = this.mSoapXMLbuilder.buildSoapXMLMessage( notifObj )
    
    if ( notifXML.isEmpty() )
    {
      return
    }
    
    /**
     * PUBLISH REQUEST XML EVENT TO MESSAGE BUS
     */
    switch (type)
    {
      case OpsTxnType.AVAILABILITY_MGMT:
        this.publishEventToMessageBus(correlationId, PersistenceMessageBusQueueNames.REQUEST_XML_QUEUE, messageType,
        notifObj.class.name, source, destination, notifXML)
        break
      case OpsTxnType.RATE_MGMT:
        this.publishEventToMessageBus(correlationId, PersistenceMessageBusQueueNames.REQUEST_XML_QUEUE, messageType,
        notifObj.class.name, source, destination, notifXML)
        break
      case OpsTxnType.RESERVATION_MGMT:
        this.publishEventToMessageBus(correlationId, PersistenceMessageBusQueueNames.READ_REQUEST_XML_QUEUE, messageType,
        notifObj.class.name, source, destination, notifXML)
        break
      case OpsTxnType.RESERVATION_NOTIFICATION_REPORT_SUCCESS_MGMT:
        this.publishEventToMessageBus(correlationId, PersistenceMessageBusQueueNames.RESERVATIONS_NOTIFICATION_REPORT_XML_SUCCESS_REQUEST, messageType,
        notifObj.class.name, source, destination, notifXML)
      case OpsTxnType.RESERVATION_NOTIFICATION_REPORT_ERRORS_MGMT:
        this.publishEventToMessageBus(correlationId, PersistenceMessageBusQueueNames.RESERVATIONS_NOTIFICATION_REPORT_XML_ERRORS_REQUEST, messageType,
        notifObj.class.name, source, destination, notifXML)
        break
    }
    
    /**
     * POST XML NOTIFICATION TO SITEMINDER FOR PROCESSING
     */
    responseXML = this.postToSiteminderEndpoint( notifXML ) { int httpStatusCode, String resXML, Throwable exception ->
      sLogger.error "*** SITEMINDER - HTTP Error Code Rcvd: ${httpStatusCode}"
      sLogger.error "*** SITEMINDER - Request to be sent: \n${notifXML}"
      
      UUID correletionId =   InvocationContext.instance.correlationId
      
      /**
       * LOG A BIG FAT MESSAGE
       */
      sLogger.error(A_BUNCH_OF_XS)
      sLogger.error("XXXXXXXXERROR POSTING TO SITEMINDER ENDPOINT FOR UUID=[${correletionId}]. HTTP STATUS CODE=[${httpStatusCode}]XXXXXXXXX", exception)
      sLogger.error (resXML)
      sLogger.error(A_BUNCH_OF_XS)
      
      /**
       * PUBLISH ERROR TO ERRLOG
       */
      publishEventToMessageBus ( correlationId, PersistenceMessageBusQueueNames.TXN_ERROR, messageType, notifObj.class.name, source, destination, exception )
      publishEventToMessageBus ( correlationId, PersistenceMessageBusQueueNames.TXN_STATUS_UPDATE, messageType, notifObj.class.name, source, destination, OpsStatuses.FAILURE )
      
      return
    }
    
    
    sLogger.debug ("[${responseXML}]")
    if ( responseXML )
    {
      /**
       * PUBLISH RESPONSE XML EVENT TO MESSAGE BUS
       */
      switch (type)
      {
        case OpsTxnType.AVAILABILITY_MGMT:
          this.publishEventToMessageBus(correlationId, PersistenceMessageBusQueueNames.RESPONSE_XML_QUEUE, messageType,
          notifObj.class.name, source, destination, notifXML)
          break
        case OpsTxnType.RATE_MGMT:
          this.publishEventToMessageBus(correlationId, PersistenceMessageBusQueueNames.RESPONSE_XML_QUEUE, messageType,
          notifObj.class.name, source, destination, notifXML)
          break
        case OpsTxnType.RESERVATION_MGMT:
          this.publishEventToMessageBus(correlationId, PersistenceMessageBusQueueNames.READ_RESPONSE_XML_QUEUE, messageType,
          notifObj.class.name, source, destination, notifXML)
          break
        case OpsTxnType.RESERVATION_NOTIFICATION_REPORT_SUCCESS_MGMT:
          this.publishEventToMessageBus(correlationId, PersistenceMessageBusQueueNames.RESERVATIONS_NOTIFICATION_REPORT_XML_SUCCESS_RESPONSE, messageType,
          notifObj.class.name, source, destination, notifXML)
        case OpsTxnType.RESERVATION_NOTIFICATION_REPORT_ERRORS_MGMT:
          this.publishEventToMessageBus(correlationId, PersistenceMessageBusQueueNames.RESERVATIONS_NOTIFICATION_REPORT_XML_ERRORS_RESPONSE, messageType,
          notifObj.class.name, source, destination, notifXML)
          break
      }
      
      
      def response = new XmlSlurper().parseText( responseXML ).declareNamespace('SOAP-ENV': 'http://schemas.xmlsoap.org/soap/envelope/' )
      
      def body = response.Body
      
      GPathResult errors = null
      
      switch (type)
      {
        case OpsTxnType.AVAILABILITY_MGMT:
          errors = body.OTA_HotelAvailNotifRS.Errors
          break
        case OpsTxnType.RATE_MGMT:
          notifObj = new RateRequest()
          errors = body.OTA_HotelRateAmountNotifRS.Errors
          break
        case OpsTxnType.RESERVATION_MGMT:
          notifObj = new ReservationRequest()
          break
      }
      
      GPathResult reservationRetrieval = body.OTA_ResRetrieveRS
      
      
      if(errors!=null && !errors.isEmpty() ){
        def errorMsg = "type = [${errors[0].Error.@Type}] code = [${errors[0].Error.@Code}] error message = [${errors[0].Error}] "
        sLogger.info(errorMsg)
        
        /**
         * UPDATE STATUS TO OpsStatuses.FAILURE
         */
        this.publishEventToMessageBus ( correlationId, PersistenceMessageBusQueueNames.TXN_STATUS_UPDATE, messageType,
            notifObj.class.name, source, destination, OpsStatuses.FAILURE )
        /**
         * PUBLISH TO ERROR LOG
         */
        this.publishEventToMessageBus ( correlationId, PersistenceMessageBusQueueNames.TXN_ERROR, messageType,
            notifObj.class.name, source, destination, errorMsg )
      }
      else if(!reservationRetrieval.isEmpty())
      {
        String jsonResponse = this.mJsonBuilder.getReservationConfirmations(reservationRetrieval)
        this.publishEventToMessageBus (correlationId, PersistenceMessageBusQueueNames.TXN_STATUS_UPDATE, messageType, notifObj.class.name,source, destination, OpsStatuses.CONFIRM_RESERVATIONS )
        this.publishEventToMessageBus (correlationId, PersistenceMessageBusQueueNames.READ_RESPONSE_JSON_QUEUE, messageType, notifObj.class.name, source, destination, jsonResponse )
      }
      else
      {
        this.publishEventToMessageBus ( correlationId, PersistenceMessageBusQueueNames.TXN_STATUS_UPDATE, messageType, notifObj.class.name, source, destination, OpsStatuses.SUCCESS )
      }
    }
  }
  
  /**
   * Helper method that POST the supplied OTA XML Service Request to the Siteminder WS Endpoint.
   * <p />
   * @param requestXML - Service Request message to POST
   */
  def String postToSiteminderEndpoint( String requestXML, Closure handlePOSTFailure )
  {
    InvocationContext context = InvocationContext.instance
    Response response
    String responseXML = null
    WebClient client = mSiteminderWSClient
    client.path( "${mSiteminderBaseURL}" )

    try
    {
      client.type( "text/xml" ).accept( "text/xml" )
      if ( sLogger.isDebugEnabled() )
      {
        sLogger.debug "*** SITEMINDER - POSTing Service Request: [${context.getSessionDataItem( "TXN_REF" )}] to Siteminder WS Endpoint: [${client.currentURI}]"
      }
      response = client.post( requestXML )
      
      if ( response.status != 200 )
      {
        InputStream inputStream = (InputStream) response.getEntity()
        responseXML = IOUtils.toString( inputStream )
        handlePOSTFailure.call( response.status, responseXML, null )
        return responseXML
      }
      
      if ( sLogger.isDebugEnabled() )
      {
        sLogger.debug "*** SITEMINDER - POSTed Service Request: [${context.getSessionDataItem( "TXN_REF" )} to Siteminder WS Endpoint: [${client.currentURI}]"
        InputStream inputStream = (InputStream) response.getEntity()
        responseXML = IOUtils.toString( inputStream )
        sLogger.debug "\n[${responseXML}]"
      }
    }
    catch ( Throwable e )
    {
      handlePOSTFailure.call( -0, e )
    }
    
    return responseXML
  }
  
  /**
   * Helper method to handle and error that occurred during POSTing to OPSARI REST Endpoint.
   * <p />
   * @param requestObj - request in Object form
   * @param errorMessage - Descriptive error message
   * @param e - Exception caught
   */
  def logAndHandleError( RateRequest requestObj, String errorMessage, Throwable e )
  {
    sLogger.error "SITEMINDER - *************************************************************************************************"
    sLogger.error "${errorMessage}"
    sLogger.error "SITEMINDER - *************************************************************************************************"
    sLogger.error errorMessage, e
  }
}
