package com.its.openpath.module.reztripsim.gds.availability

import com.dyuproject.protostuff.JsonIOUtil
import com.its.openpath.module.opscommon.model.messaging.ops.AvailabilityRequest
import com.its.openpath.module.opscommon.model.messaging.ops.AvailabilityResponse
import com.its.openpath.module.opscommon.model.messaging.ops.BaseRoomStayRate
import com.its.openpath.module.opscommon.model.messaging.ops.ErrorMessage
import com.its.openpath.module.opscommon.model.messaging.ops.ErrorResponse
import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode
import com.its.openpath.module.opscommon.model.messaging.ops.ProductType
import com.its.openpath.module.opscommon.model.messaging.ops.RoomAvailabilityRequest
import com.its.openpath.module.opscommon.model.messaging.ops.RoomAvailabilityResponse
import com.its.openpath.module.opscommon.model.messaging.ops.RoomRate
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStay
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStayRate
import com.its.openpath.module.opscommon.model.messaging.ops.Stay
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import com.its.openpath.module.opscommon.model.messaging.ops.AvailabilityRatePlan
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStayGuestCount
import com.its.openpath.module.opscommon.model.messaging.ops.PropertyInfo

/**
 * <code>GDSAreaAvailabilityResourceHandler</code>
 * <p/>
 * The REST Resource handler that handles Area Availability Service Requests. Accepts requests in OpenPath JSON format
 * and sends a response in the same format simulating the RezTrip CRS.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service("GDSAreaAvailabilityResourceHandler")
@ManagedResource('OPENPATH:name=/module/reztripsim/gds/availability/GDSAreaAvailabilityResourceHandler')
class GDSAreaAvailabilityResourceHandler
{
  private static final Logger sLogger = LoggerFactory.getLogger( GDSAreaAvailabilityResourceHandler.class.name )

  private DateTimeFormatter mDateTimeFormatter = DateTimeFormat.forPattern( "yyyy-MM-dd" )


  /**
   * Constructor
   */
  GDSAreaAvailabilityResourceHandler( )
  {
    sLogger.info 'Instantiated ...'
  }

  /**
   * Handle Area Availability search requests.
   * <p />
   * @param servletRequest - Contains the incoming service request
   * @return Response - The Availability Search response built
   */
  def Response search( HttpServletRequest servletRequest )
  {
    String responseJSON = null, requestJSON = null
    Response responseObj;
    AvailabilityRequest requestObj = new AvailabilityRequest()

    // Deserialize the JSON request to its object form
    try
    {
      requestJSON = servletRequest.inputStream.text
      if ( sLogger.isDebugEnabled() )
      {
        sLogger.debug "*** RezTripSim - Rcvd a new AREA AVAILABILITY Service Request ..."
        sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
        if ( sLogger.isDebugEnabled() )
        {
          sLogger.debug "*** RezTripSim - The new Availability JSON message before parsing: \n[${requestJSON}]"
        }
      }
      JsonIOUtil.mergeFrom( requestJSON.bytes, requestObj, AvailabilityRequest.schema, false )
    }
    catch ( Throwable e )
    {
      responseJSON = this.logAndBuildErrorResponse( null,
        "RezTripSim - Couldn't parse the OPS JSON request received", e, OpsErrorCode.SERVICE_REQUEST_FORMAT_ERROR )
    }

    if ( responseJSON == null )
    {
      responseJSON = validate( requestObj )
      if ( responseJSON == null )
      {
        responseJSON = this.createOpenPathJSONResponse( requestObj )
      }
    }

    responseObj = Response.ok( responseJSON ).status( 200 ).build();
    if ( sLogger.isDebugEnabled() )
    {
      sLogger.debug "RezTripSim - OPS JSON Response for the Area Availability Request is: \n[${responseJSON}"
    }

    return responseObj
  }

  /**
   * Simple validation of the check-in and check-out dates so that a proper "Error" response can be sent back.
   * <p />
   * @param requestObjJSON - Area Pro Avl Req received
   * @return String - a JSON Error Message if the Check-in or Check-out dates are in correct
   */
  def private String validate( AvailabilityRequest requestObjJSON )
  {
    RoomAvailabilityRequest request = requestObjJSON.requestData
    Stay stay = request.stay

    try
    {
      if ( stay.start.isEmpty() )
      {
        return this.logAndBuildErrorResponse( requestObjJSON, "Check In date provided is empty", null,
          OpsErrorCode.INVALID_CHECK_IN_OUT_DATE )
      }

      // YYYY/MM/DD
      DateTime startDate = mDateTimeFormatter.parseDateTime( stay.start )
      DateTime currentDate = new DateTime()
      if ( currentDate.year != startDate.year || startDate.monthOfYear < currentDate.monthOfYear )
      {
        sLogger.info( "Current year: ${currentDate.year}, start year: ${startDate.year}, current month: ${currentDate.monthOfYear}, " +
          "start month: ${startDate.monthOfYear}" )
        return this.logAndBuildErrorResponse( requestObjJSON, "Check-in date ${stay.start} provided cannot be in the past", null,
          OpsErrorCode.INVALID_CHECK_IN_OUT_DATE )
      }

      DateTime endDate = mDateTimeFormatter.parseDateTime( stay.end )
      if ( currentDate.year != endDate.year || endDate.monthOfYear < currentDate.monthOfYear )
      {
        sLogger.info( "Current year: ${currentDate.year}, end year: ${endDate.year}, current month: ${currentDate.monthOfYear}, " +
          "end month: ${endDate.monthOfYear}" )
        return this.logAndBuildErrorResponse( requestObjJSON, "Check-out date ${stay.end} provided cannot be in the past", null,
          OpsErrorCode.INVALID_CHECK_IN_OUT_DATE )
      }
    }
    catch ( Throwable e )
    {
      return this.logAndBuildErrorResponse( null,
        "RezTripSim - Couldn't validate the OPS JSON request received: ${e.message}", e, OpsErrorCode.SERVICE_REQUEST_FORMAT_ERROR )
    }

    return null
  }

  /**
   * Helper method to log any exception during parsing the Availability Request received or building an Availability
   * Response and send a format a JSON Availability Response with the error status set.
   * <p />
   * @param requestObj - Availability request in Object form
   * @param errorMessage - Descriptive error message to set in the header
   * @param e - Exception caught
   * @param errorCode - Error code to set in the header
   * @return String - Availability Response JSON stream
   */
  def String logAndBuildErrorResponse( AvailabilityRequest requestObj, String errorMessage, Throwable e, OpsErrorCode errorCode )
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
    availResponse.productType = (requestObj == null) ? ProductType.UNKNOWN : requestObj.productType
    availResponse.errorResponse = errorResponse
    StringWriter writer = new StringWriter()
    JsonIOUtil.writeTo( writer, availResponse, availResponse.cachedSchema(), false );

    writer.toString();
  }


  /**
   * Helper method to build the JSON Hotel Availability to be sent back. Randomly sends a valid Availability Response or no
   * Availability Response with an Error Message.
   * <p />
   * @param jsonRequest - Service Request received
   * @return String - The response JSON string
   */
  private String createOpenPathJSONResponse( AvailabilityRequest requestObjJSON )
  {
    AvailabilityResponse availResponse = new AvailabilityResponse()
    StringWriter writer = new StringWriter()

    try
    {
      switch ( new Random().nextInt( 2 ) )
      {
        case 0:
          populateValidAvailabilityResponse( requestObjJSON, availResponse )
          break

        case 1:
          populateErrorResponse( requestObjJSON, availResponse )
          break
      }

      JsonIOUtil.writeTo( writer, availResponse, availResponse.cachedSchema(), false );
    }
    catch ( Throwable e )
    {
      return this.logAndBuildErrorResponse( requestObjJSON,
        "RezTripSim - Couldn't create an OPS JSON response to be sent back", e, OpsErrorCode.SERVICE_RESPONSE_FORMAT_ERROR )
    }

    sLogger.info( "RezTripSim - Successfully created the OPS JSON response to be sent back ..." )
    return writer.toString()
  }

  /**
   * Helper method to build a JSON Hotel Availability response to be sent back.
   * <p />
   * @param requestObjJSON - Service Request received
   * @param availResponse - Object to set the JSON response
   */
  private populateErrorResponse( AvailabilityRequest requestObjJSON, AvailabilityResponse availResponse )
  {
    availResponse.productType = requestObjJSON.productType
    List<ErrorMessage> errorMessageList = []
    ErrorResponse errorResponse = new ErrorResponse()
    availResponse.errorResponse = errorResponse
    errorResponse.errorMessagesList = errorMessageList
    ErrorMessage errorMessage = new ErrorMessage()
    errorMessageList << errorMessage

    RoomAvailabilityRequest request = requestObjJSON.requestData
    Stay stay = request.stay
    errorMessage.errorCode = OpsErrorCode.NO_INVENTORY_AVAILABLE
    errorMessage.errorMessage = "No inventory availability for the specified period: [${stay.start}] to [${stay.end}]"
  }

  /**
   * Helper method to build a JSON Hotel Availability response to be sent back.
   * <p />
   * @param requestObjJSON - Service Request received
   * @param availResponse - Object to set the JSON response
   */
  private populateValidAvailabilityResponse( AvailabilityRequest requestObjJSON, AvailabilityResponse availResponse )
  {
    RoomAvailabilityResponse roomAvailabilityResponse = new RoomAvailabilityResponse()
    availResponse.responseData = roomAvailabilityResponse
    availResponse.productType = requestObjJSON.productType
    availResponse.responseData = roomAvailabilityResponse
    roomAvailabilityResponse.extSysRefId = requestObjJSON.requestData.extSysRefId
    roomAvailabilityResponse.extSysTimestamp = requestObjJSON.requestData.extSysTimestamp
    roomAvailabilityResponse.isSuccess = true

    List<RoomStay> roomStayList = new ArrayList<RoomStay>()
    roomAvailabilityResponse.roomStaysList = roomStayList
    RoomStay roomStay1 = new RoomStay()
    roomStayList.add( roomStay1 )
    populateRoomStay1( roomStay1 )

    RoomStay roomStay2 = new RoomStay()
    roomStayList.add( roomStay2 )
    populateRoomStay2( roomStay2 )

    RoomStay roomStay3 = new RoomStay()
    roomStayList.add( roomStay3 )
    roomStay3.availabilityStatus = "NoAvailability"
    PropertyInfo propertyInfo = new PropertyInfo()
    roomStay3.basicPropertyInfo = propertyInfo
    propertyInfo.chainCode = "XY"
    propertyInfo.hotelCode = "EF9012"
    propertyInfo.name = "Bubba Inn"
    propertyInfo.city = "My City"
    propertyInfo.state = "GA"
    propertyInfo.country = "USA"
  }

  /**
   * Populate Room Stay info and its child elements.
   * <p />
   * @param roomStay1 - Room Stay object to populate
   */
  def populateRoomStay1( RoomStay roomStay1 )
  {
    roomStay1.availabilityStatus = "AvailableForSale"

    List<AvailabilityRatePlan> ratePlanList = new ArrayList<AvailabilityRatePlan>()
    roomStay1.ratePlansList = ratePlanList
    AvailabilityRatePlan ratePlan1 = new AvailabilityRatePlan()
    ratePlanList.add( ratePlan1 )
    ratePlan1.code = "PEG"
    ratePlan1.availabilityStatus = "NoAvailability"
    AvailabilityRatePlan ratePlan2 = new AvailabilityRatePlan()
    ratePlanList.add( ratePlan2 )
    ratePlan2.code = "PRO"
    ratePlan2.availabilityStatus = "AvailableForSale"

    // Populate Room Stay Rates
    populateRoomStay1Rates( roomStay1 )

    List<RoomStayGuestCount> roomStayGuestCountList = new ArrayList<RoomStayGuestCount>()
    roomStay1.guestCountsList = roomStayGuestCountList
    RoomStayGuestCount roomStayGuestCount1 = new RoomStayGuestCount()
    roomStayGuestCountList.add( roomStayGuestCount1 )
    roomStayGuestCount1.ageQualifier = "10"
    roomStayGuestCount1.count = "2"
    RoomStayGuestCount roomStayGuestCount2 = new RoomStayGuestCount()
    roomStayGuestCountList.add( roomStayGuestCount2 )
    roomStayGuestCount2.ageQualifier = "8"
    roomStayGuestCount2.count = "1"

    PropertyInfo propertyInfo = new PropertyInfo()
    roomStay1.basicPropertyInfo = propertyInfo
    propertyInfo.chainCode = "XY"
    propertyInfo.hotelCode = "AB1234"
    propertyInfo.name = "Holiday Inn"
    propertyInfo.city = "My City"
    propertyInfo.state = "GA"
    propertyInfo.country = "USA"
  }

  /**
   * Populate room stay rate data in the given RoomStay object
   * <p />
   * @param roomStay - object to populate
   */
  def private populateRoomStay1Rates( RoomStay roomStay )
  {
    List<RoomStayRate> roomStayRateList = new ArrayList<RoomStayRate>()
    roomStay.roomStayRatesList = roomStayRateList

    RoomStayRate roomStayRate = new RoomStayRate()
    roomStayRateList.add( roomStayRate )

    // 1st Rate
    List<RoomRate> roomRateList = new ArrayList<RoomRate>()
    roomStayRate.ratesList = roomRateList
    RoomRate roomRate1 = new RoomRate()
    roomRateList.add( roomRate1 )
    roomRate1.rateMode = "1"

    BaseRoomStayRate baseRoomStayRate1 = new BaseRoomStayRate()
    roomRate1.baseRate = baseRoomStayRate1
    baseRoomStayRate1.amountBeforeTax = "125.00"
    baseRoomStayRate1.currencyCode = "GBP"
    baseRoomStayRate1.calculationMethod = "3"
    baseRoomStayRate1.commissionable = "Commissionalbe"

    // 2nd Rate
    RoomRate roomRate2 = new RoomRate()
    roomRateList.add( roomRate2 )
    roomRate2.rateMode = "2"
    BaseRoomStayRate baseRoomStayRate2 = new BaseRoomStayRate()
    roomRate2.baseRate = baseRoomStayRate2
    baseRoomStayRate2.amountBeforeTax = "215.00"
    baseRoomStayRate2.currencyCode = "GBP"
    baseRoomStayRate2.calculationMethod = "3"
    baseRoomStayRate2.commissionable = "Commissionalbe"
  }

  /**
   * Populate Room Stay info and its child elements.
   * <p />
   * @param roomStay2 - Room Stay object to populate
   */
  def populateRoomStay2( RoomStay roomStay2 )
  {
    roomStay2.availabilityStatus = "ChangeDuringStay"

    List<AvailabilityRatePlan> ratePlanList = new ArrayList<AvailabilityRatePlan>()
    roomStay2.ratePlansList = ratePlanList
    AvailabilityRatePlan ratePlan1 = new AvailabilityRatePlan()
    ratePlanList.add( ratePlan1 )
    ratePlan1.code = "PEG"
    ratePlan1.availabilityStatus = "AvailabileForSale"
    AvailabilityRatePlan ratePlan2 = new AvailabilityRatePlan()
    ratePlanList.add( ratePlan2 )
    ratePlan2.code = "PRO"
    ratePlan2.availabilityStatus = "AvailableForSale"

    // Populate Room Stay Rates
    populateRoomStay2Rates( roomStay2 )

    List<RoomStayGuestCount> roomStayGuestCountList = new ArrayList<RoomStayGuestCount>()
    roomStay2.guestCountsList = roomStayGuestCountList
    RoomStayGuestCount roomStayGuestCount1 = new RoomStayGuestCount()
    roomStayGuestCountList.add( roomStayGuestCount1 )
    roomStayGuestCount1.ageQualifier = "10"
    roomStayGuestCount1.count = "2"
    RoomStayGuestCount roomStayGuestCount2 = new RoomStayGuestCount()
    roomStayGuestCountList.add( roomStayGuestCount2 )
    roomStayGuestCount2.ageQualifier = "8"
    roomStayGuestCount2.count = "1"

    PropertyInfo propertyInfo = new PropertyInfo()
    roomStay2.basicPropertyInfo = propertyInfo
    propertyInfo.chainCode = "XY"
    propertyInfo.hotelCode = "CD5678"
    propertyInfo.name = "Hampton Inn"
    propertyInfo.city = "My City"
    propertyInfo.state = "GA"
    propertyInfo.country = "USA"
  }

  /**
   * Populate room stay rate data in the given RoomStay object
   * <p />
   * @param roomStay - object to populate
   */
  def private populateRoomStay2Rates( RoomStay roomStay )
  {
    List<RoomStayRate> roomStayRateList = new ArrayList<RoomStayRate>()
    roomStay.roomStayRatesList = roomStayRateList

    RoomStayRate roomStayRate = new RoomStayRate()
    roomStayRateList.add( roomStayRate )

    // 1st Rate
    List<RoomRate> roomRateList = new ArrayList<RoomRate>()
    roomStayRate.ratesList = roomRateList
    RoomRate roomRate1 = new RoomRate()
    roomRateList.add( roomRate1 )
    roomRate1.rateMode = "1"

    BaseRoomStayRate baseRoomStayRate1 = new BaseRoomStayRate()
    roomRate1.baseRate = baseRoomStayRate1
    baseRoomStayRate1.amountBeforeTax = "80.00"
    baseRoomStayRate1.currencyCode = "GBP"
    baseRoomStayRate1.calculationMethod = "3"
    baseRoomStayRate1.commissionable = "Commissionalbe"

    // 2nd Rate
    RoomRate roomRate2 = new RoomRate()
    roomRateList.add( roomRate2 )
    roomRate2.rateMode = "2"
    BaseRoomStayRate baseRoomStayRate2 = new BaseRoomStayRate()
    roomRate2.baseRate = baseRoomStayRate2
    baseRoomStayRate2.amountBeforeTax = "170.00"
    baseRoomStayRate2.currencyCode = "GBP"
    baseRoomStayRate2.calculationMethod = "3"
    baseRoomStayRate2.commissionable = "Commissionalbe"
  }

}
