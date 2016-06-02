package com.its.openpath.module.opsari.reztrip.handler

import com.dyuproject.protostuff.JsonIOUtil
import com.its.openpath.module.opscommon.comm.bus.IMessageBus
import com.its.openpath.module.opscommon.model.messaging.ops.AvailabilityRequest
import com.its.openpath.module.opscommon.model.messaging.ops.AvailabilityResponse
import com.its.openpath.module.opscommon.model.messaging.ops.ErrorMessage
import com.its.openpath.module.opscommon.model.messaging.ops.ErrorResponse
import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode
import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.model.messaging.ops.ProductType
import com.its.openpath.module.opscommon.model.messaging.ops.RoomAvailabilitySearchType
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateManagementType
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateRequest
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ReservationManagementType
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ReservationRequest
import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.opscommon.util.PersistenceMessageBusQueueNames
import com.its.openpath.module.opscommon.util.TimeUUIDUtils
import org.apache.cxf.helpers.IOUtils
import org.apache.cxf.jaxrs.client.WebClient
import org.perf4j.StopWatch
import org.perf4j.slf4j.Slf4JStopWatch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jmx.export.annotation.ManagedAttribute

import javax.ws.rs.core.Response

/**
 * <code>AbstractServiceRequestHandler</code>
 * <p/>
 * Base class of all 'Handler' classes. Contains methods common to all subclasses.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

abstract class AbstractServiceRequestHandler
{
  private static final Logger sLogger = LoggerFactory.getLogger( AbstractServiceRequestHandler.class.name )


  private @Value("#{runtimeProperties['reztrip.singlePropertyAvailabilitySearch.url']}")
  String mRezTripSinglePropAvlServiceURL;

  private @Value("#{runtimeProperties['reztrip.areaAvailabilitySearch.url']}")
  String mRezTripAreaAvlServiceURL;

  private @Value("#{runtimeProperties['reztrip.reservationManagement.url']}")
  String mRezTripReservationMgtServiceURL;

  private @Value("#{runtimeProperties['reztrip.rateManagement.url']}")
  String mRezTripRateMgtServiceURL;


  // The URL or POST header values doesn't change per-request, so using an instance like this is thread safe
  private WebClient mRezTripSinglePropAvlRESTClient = WebClient.create( "${mRezTripSinglePropAvlServiceURL}" )
  private WebClient mRezTripAreaAvlRESTClient = WebClient.create( "${mRezTripAreaAvlServiceURL}" )
  private WebClient mRezTripResMgtRESTClient = WebClient.create( "${mRezTripReservationMgtServiceURL}" )
  private WebClient mRezTripRateMgtRESTClient = WebClient.create( "${mRezTripRateMgtServiceURL}" )

  @Autowired(required = true)
  protected IMessageBus mOpsMessageBus

  StopWatch mStopWatch = new Slf4JStopWatch()



  @ManagedAttribute
  def void setSinglePropertyAvlServiceURL( String url )
  {
    mRezTripSinglePropAvlRESTClient = WebClient.create( "${url}" )
    mRezTripSinglePropAvlServiceURL = url
  }

  @ManagedAttribute
  def String getSinglePropertyAvlServiceURL( )
  {
    mRezTripSinglePropAvlServiceURL
  }

  @ManagedAttribute
  def void setAreaAvlServiceURL( String url )
  {
    mRezTripAreaAvlRESTClient = WebClient.create( "${url}" );
    mRezTripAreaAvlServiceURL = url;
  }

  @ManagedAttribute
  def String getAreaAvlServiceURL( )
  {
    mRezTripAreaAvlServiceURL
  }

  @ManagedAttribute
  def void setReservationMgtServiceURL( String url )
  {
    mRezTripResMgtRESTClient = WebClient.create( "${url}" )
    mRezTripReservationMgtServiceURL = url
  }

  @ManagedAttribute
  def String getReservationMgtServiceURL( )
  {
    mRezTripReservationMgtServiceURL
  }

  @ManagedAttribute
  def void setRateMgtServiceURL( String url )
  {
    mRezTripRateMgtRESTClient = WebClient.create( "${url}" )
    mRezTripRateMgtServiceURL = url
  }

  @ManagedAttribute
  def String getRateMgtServiceURL( )
  {
    mRezTripRateMgtServiceURL
  }

  /**
   * Helper method that POST the supplied JSON encoded Service Request to the RezTrip CRS REST Endpoint to perform
   * Single Property and Area Availability searches.
   * <p />
   * @param requestJSON - JSON Service Request message in OpenPath format
   * @param requestObj - Deserialized Service Request
   * @return String - JSON Service Response message returned by the RezTrip CRS or a Response Message with error status
   */
  def String postAvlRequestToRezTripCRS( String requestJSON, AvailabilityRequest requestObj, Closure handleResponse, Closure handlePOSTFailure )
  {
    Response response
    String responseJSON
    InvocationContext context = InvocationContext.instance
    WebClient client = null
    handlePOSTFailure.memoize()

    try
    {
      /**
       * SEND REQUEST JSON EVENT TO MESSAGE BUS TO BE STORED IN DATABASE HERE
       */
      UUID uuid = context.correlationId
      OpsMessage reqMsg = new OpsMessage()
      reqMsg.correlationId = uuid.toString()
      reqMsg.correlationIdBytes = context.correlationIdByteString
      reqMsg.timestamp = TimeUUIDUtils.getTimeFromUUID( uuid )
      reqMsg.data = requestJSON
      mOpsMessageBus.queueMessage( PersistenceMessageBusQueueNames.REQUEST_JSON_QUEUE, reqMsg )

      switch ( requestObj.requestData.searchType )
      {
        case RoomAvailabilitySearchType.SINGLE_PROPERTY:
          client = mRezTripSinglePropAvlRESTClient
          client.path( "${mRezTripSinglePropAvlServiceURL}" )
          break

        case RoomAvailabilitySearchType.AREA_AVAILABILITY:
          client = mRezTripAreaAvlRESTClient
          client.path( "${mRezTripAreaAvlServiceURL}" )
          break
      }
      client.type( "application/json" ).accept( "application/json" )
      if ( sLogger.isDebugEnabled() )
      {
        sLogger.debug "*** OPSARI - POSTing Service Request: [${context.getSessionDataItem( "TXN_REF" )}] to RezTrip REST Endpoint: [${client.currentURI}]"
      }

      mStopWatch.start( "PEGASUS.ARI.${requestObj.requestData.searchType}" )
      response = client.post( requestJSON )
      mStopWatch.stop( "PEGASUS.ARI.${requestObj.requestData.searchType}", "Total elapsed time for: ${requestObj.requestData.searchType}" )

      if ( response.status != 200 )
      {
        responseJSON = handlePOSTFailure( client.currentURI.toString(), response.status, null )
      }
      else
      {
        if ( sLogger.isDebugEnabled() )
        {
          sLogger.debug "*** OPSARI - POSTed Service Request: [${context.getSessionDataItem( "TXN_REF" )} to RezTrip REST Endpoint: [${client.currentURI}]"
        }
        InputStream inputStream = (InputStream) response.getEntity()
        responseJSON = handleResponse( IOUtils.toString( inputStream ) )
      }
    }
    catch ( Throwable e )
    {
      responseJSON = handlePOSTFailure( client.currentURI.toString(), -0, e )
    }

    return responseJSON
  }

  /**
   * Helper method that POST the supplied JSON encoded Service Request to the RezTrip CRS REST Endpoint to manage
   * Reservation Create/Update/Delete operations.
   * <p />
   * @param requestJSON - JSON Service Request message in OpenPath format
   * @param requestObj - Deserialized Service Request
   * @return String - JSON Service Response message returned by the RezTrip CRS or a Response Message with error status
   */
  def String postReservationMgtRequestToRezTripCRS( String requestJSON, ReservationRequest requestObj, Closure handleResponse, Closure handlePOSTFailure )
  {
    Response response
    String responseJSON
    InvocationContext context = InvocationContext.instance
    WebClient client = null
    try
    {
      client = mRezTripResMgtRESTClient
      client.path( "${mRezTripReservationMgtServiceURL}" )
      client.type( "application/json" ).accept( "application/json" )
      if ( sLogger.isDebugEnabled() )
      {
        sLogger.debug "*** OPSARI - POSTing Service Request: [${context.getSessionDataItem( "TXN_REF" )}] to RezTrip REST Endpoint: [${client.currentURI}]"
      }

      handlePOSTFailure.delegate = this
      ReservationManagementType reservationManagementType = requestObj.requestData.reservationMgtType
      mStopWatch.start( "PEGASUS.RES.${reservationManagementType}" )
      response = client.post( requestJSON )
      mStopWatch.stop( "PEGASUS.RES.${reservationManagementType}", "Total elapsed time for: ${reservationManagementType}" )

      if ( response.status != 200 )
      {
        responseJSON = handlePOSTFailure( client.currentURI.toString(), response.status, null )
      }
      else
      {
        if ( sLogger.isDebugEnabled() )
        {
          sLogger.debug "*** OPSARI - POSTed Service Request: [${context.getSessionDataItem( "TXN_REF" )} to RezTrip REST Endpoint: [${client.currentURI}]"
        }
        InputStream inputStream = (InputStream) response.getEntity()
        responseJSON = handleResponse( IOUtils.toString( inputStream ) )
      }
    }
    catch ( Throwable e )
    {
      responseJSON = handlePOSTFailure( client.currentURI.toString(), -0, e )
    }

    return responseJSON
  }

  /**
   * Helper method that POST the supplied JSON encoded Service Request to the RezTrip CRS REST Endpoint to manage
   * Rate Management operations.
   * <p />
   * @param requestJSON - JSON Service Request message in OpenPath format
   * @param requestObj - Deserialized Service Request
   * @return String - JSON Service Response message returned by the RezTrip CRS or a Response Message with error status
   */
  def String postRateMgtRequestToRezTripCRS( String requestJSON, RateRequest requestObj, Closure handleResponse, Closure handlePOSTFailure )
  {
    Response response
    String responseJSON
    InvocationContext context = InvocationContext.instance
    WebClient client = null
    try
    {
      client = mRezTripRateMgtRESTClient
      client.path( "${mRezTripRateMgtServiceURL}" )
      client.type( "application/json" ).accept( "application/json" )
      if ( sLogger.isDebugEnabled() )
      {
        sLogger.debug "*** OPSARI - POSTing Service Request: [${context.getSessionDataItem( "TXN_REF" )}] to RezTrip REST Endpoint: [${client.currentURI}]"
      }

      handlePOSTFailure.delegate = this
      RateManagementType rateManagementType = requestObj.requestData.rateManagementType
      mStopWatch.start( "PEGASUS.RATE.${rateManagementType}" )
      response = client.post( requestJSON )
      mStopWatch.stop( "PEGASUS.RATE.${rateManagementType}", "Total elapsed time for: ${rateManagementType}" )

      if ( response.status != 200 )
      {
        responseJSON = handlePOSTFailure( client.currentURI.toString(), response.status, null )
      }
      else
      {
        if ( sLogger.isDebugEnabled() )
        {
          sLogger.debug "*** OPSARI - POSTed Service Request: [${context.getSessionDataItem( "TXN_REF" )} to RezTrip REST Endpoint: [${client.currentURI}]"
        }
        InputStream inputStream = (InputStream) response.getEntity()
        responseJSON = handleResponse( IOUtils.toString( inputStream ) )
      }
    }
    catch ( Throwable e )
    {
      responseJSON = handlePOSTFailure( client.currentURI.toString(), -0, e )
    }

    return responseJSON
  }

  /**
   * Helper method to log any exception during parsing the Availability Request received or building an Availability
   * Response, and send a format a JSON Availability Response with the error status set.
   * <p />
   * @param productType - Product type to set in the response
   * @param errorMessage - Descriptive error message to set in the header
   * @param e - Exception caught
   * @param errorCode - Error code to set in the header
   * @return String - Availability Response JSON stream
   */
  def String logAndBuildErrorResponse( ProductType productType,
    String errorMessage, Throwable e, OpsErrorCode errorCode )
  {
    sLogger.error "*************************************************************************************************"
    sLogger.error "${errorMessage}"
    sLogger.error "*************************************************************************************************"
    sLogger.error errorMessage, e

    ErrorResponse errorResponse = new ErrorResponse()
    List<ErrorMessage> errorMessageList = new ArrayList<ErrorMessage>()
    errorResponse.errorMessagesList = errorMessageList
    ErrorMessage errorMessage1 = new ErrorMessage()
    errorMessage1.errorCode = errorCode
    errorMessage1.errorMessage = errorMessage
    errorMessageList.add( errorMessage1 )

    AvailabilityResponse availResponse = new AvailabilityResponse()
    availResponse.productType = productType
    availResponse.errorResponse = errorResponse
    StringWriter writer = new StringWriter()
    JsonIOUtil.writeTo( writer, availResponse, availResponse.cachedSchema(), false );

    writer.toString();
  }

  /**
   * Helper method to publish the response to the requested topic on the OPS Message Bus.
   * <p />
   * @param responseJSON - JSON response
   * @param opsReqMessage - Original request received
   */
  def publishResponseOnOpsBus( String responseJSON, OpsMessage opsReqMessage )
  {
    OpsMessage opsRspMessage = new OpsMessage()
    opsRspMessage.messageType = opsReqMessage.messageType
    opsRspMessage.correlationId = opsReqMessage.correlationId
    opsRspMessage.correlationIdBytes = opsReqMessage.correlationIdBytes
    opsRspMessage.timestamp = TimeUUIDUtils.getTimeFromUUID( opsReqMessage.correlationIdBytes )
    opsRspMessage.data = responseJSON

    mOpsMessageBus.queueMessage( opsReqMessage.correlationId, opsRspMessage )
    mOpsMessageBus.queueMessage( PersistenceMessageBusQueueNames.RESPONSE_JSON_QUEUE, opsRspMessage )
  }

}
