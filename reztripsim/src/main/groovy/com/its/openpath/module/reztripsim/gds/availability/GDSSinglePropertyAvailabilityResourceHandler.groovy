package com.its.openpath.module.reztripsim.gds.availability

import com.dyuproject.protostuff.JsonIOUtil
import com.its.openpath.module.opscommon.model.messaging.ops.AcceptablePayment
import com.its.openpath.module.opscommon.model.messaging.ops.AvailabilityRatePlan
import com.its.openpath.module.opscommon.model.messaging.ops.AvailabilityRequest
import com.its.openpath.module.opscommon.model.messaging.ops.AvailabilityResponse
import com.its.openpath.module.opscommon.model.messaging.ops.BaseRoomStayRate
import com.its.openpath.module.opscommon.model.messaging.ops.PropertyInfo
import com.its.openpath.module.opscommon.model.messaging.ops.ErrorMessage
import com.its.openpath.module.opscommon.model.messaging.ops.ErrorResponse
import com.its.openpath.module.opscommon.model.messaging.ops.HotelAmenity
import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode
import com.its.openpath.module.opscommon.model.messaging.ops.PaymentPolicies
import com.its.openpath.module.opscommon.model.messaging.ops.ProductType
import com.its.openpath.module.opscommon.model.messaging.ops.RatePlanInclusion
import com.its.openpath.module.opscommon.model.messaging.ops.Room
import com.its.openpath.module.opscommon.model.messaging.ops.RoomAmenity
import com.its.openpath.module.opscommon.model.messaging.ops.RoomAvailabilityRequest
import com.its.openpath.module.opscommon.model.messaging.ops.RoomAvailabilityResponse
import com.its.openpath.module.opscommon.model.messaging.ops.RoomRate
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStay
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStayAdditionalFee
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStayAdditionalGuestFee
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStayCancelPenalty
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStayCancelPenaltyDeadline
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStayGuaranteeDeadline
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStayGuaranteePayment
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStayGuaranteePaymentAmount
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStayGuestCount
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStayRate
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStayTax
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStayTimeSpan
import com.its.openpath.module.opscommon.model.messaging.ops.RoomStayTotal
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

/**
 * <code>GDSSinglePropertyAvailabilityResourceHandler</code>
 * <p/>
 * The REST Resource handler that handles Single Property Availability Service Requests. Accepts requests in OpenPath JSON format
 * and sends a response in the same format simulating the RezTrip CRS.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service("GDSSinglePropertyAvailabilityResourceHandler")
@ManagedResource('OPENPATH:name=/module/reztripsim/gds/availability/GDSSinglePropertyAvailabilityResourceHandler')
class GDSSinglePropertyAvailabilityResourceHandler
{
  private static final Logger sLogger = LoggerFactory.getLogger( GDSSinglePropertyAvailabilityResourceHandler.class.name )

  private DateTimeFormatter mDateTimeFormatter = DateTimeFormat.forPattern( "yyyy-MM-dd" )


  /**
   * Constructor
   */
  GDSSinglePropertyAvailabilityResourceHandler( )
  {
    sLogger.info 'Instantiated ...'
  }

  /**
   * Handle Single Property Availability search requests.
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
        sLogger.debug "*** RezTripSim - Rcvd a new Single Property Availability Service Request ..."
        sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
        if ( sLogger.isDebugEnabled() )
        {
          sLogger.debug "*** RezTripSim -  The new Availability JSON message before parsing: \n[${requestJSON}]"
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
      sLogger.debug "RezTripSim - OPS JSON Response for the Single Prop Availability Request is: \n[${responseJSON}"
    }

    return responseObj
  }

  /**
   * Simple validation of the check-in and check-out dates so that a proper "Error" response can be sent back.
   * <p />
   * @param requestObjJSON - Single Pro Avl Req received
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
   * Helper method to build the JSON Hotel Availability to be sent back.
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
      RoomAvailabilityResponse roomAvailabilityResponse = new RoomAvailabilityResponse()
      availResponse.productType = ProductType.HOTEL_ROOM
      availResponse.responseData = roomAvailabilityResponse
      roomAvailabilityResponse.extSysRefId = requestObjJSON.requestData.extSysRefId
      roomAvailabilityResponse.extSysTimestamp = requestObjJSON.requestData.extSysTimestamp
      roomAvailabilityResponse.isSuccess = true

      List<RoomStay> roomStayList = new ArrayList<RoomStay>()
      roomAvailabilityResponse.roomStaysList = roomStayList
      RoomStay roomStay = new RoomStay()
      roomStay.availabilityStatus = "AvailableForSale"
      roomStayList.add( roomStay )
      List<Room> roomList = []
      Room room = new Room()
      roomStay.roomsList = roomList
      roomList << room
      room.typeId = "A1K"
      room.category = "Economy"
      room.description = "Superior King Room"
      room.isAlternate = "false"
      room.numberOfUnits = "1"
      room.numberOfUnitsMatchType = "Yes"
      room.adultOccupancyMatchType = "Yes"
      room.childOccupancyMatchType = "Yes"
      //room.bedTypeMatchType = "Yes"

      List<RoomAmenity> roomAmenityList = new ArrayList<RoomAmenity>()
      room.amenitiesList = roomAmenityList
      RoomAmenity amenity1 = new RoomAmenity()
      amenity1.id = "Cribs"
      amenity1.quantity = "1"
      amenity1.includedInRate = "true"
      amenity1.needConfirmation = "true"
      amenity1.existsCode = "Yes"
      roomAmenityList.add( amenity1 )

      RoomAmenity amenity2 = new RoomAmenity()
      amenity2.id = "King Bed"
      amenity2.quantity = "1"
      amenity2.includedInRate = "true"
      amenity2.needConfirmation = "false"
      amenity2.existsCode = "Yes"
      roomAmenityList.add( amenity2 )

      List<AvailabilityRatePlan> ratePlanList = new ArrayList<AvailabilityRatePlan>()
      roomStay.ratePlansList = ratePlanList
      AvailabilityRatePlan ratePlan = new AvailabilityRatePlan()
      ratePlanList.add( ratePlan )
      ratePlan.id = "654321"
      ratePlan.code = "P123"
      ratePlan.description = "Awesome rate plan"
      ratePlan.marketCode = "S"
      ratePlan.availabilityStatus = "AvailableForSale"
      ratePlan.effectiveDate = "2010-12-31-12:00"
      ratePlan.expiryDate = "2011-06-15-12:00"
      ratePlan.idRequiredAtCheckin = "true"
      ratePlan.promotionalText = "Great savings available for weekend breaks during the summer"
      List<RatePlanInclusion> ratePlanInclusionList = new ArrayList<RatePlanInclusion>()
      ratePlan.inclusionsList = ratePlanInclusionList
      RatePlanInclusion inclusion = new RatePlanInclusion()
      ratePlanInclusionList.add( inclusion )
      inclusion.id = "Tax"
      inclusion.description = "Tax Included"
      inclusion.value = false

      // Populate Room Stay Rates
      populateRoomStayRates( roomStay )

      List<RoomStayGuestCount> roomStayGuestCountList = new ArrayList<RoomStayGuestCount>()
      roomStay.guestCountsList = roomStayGuestCountList
      RoomStayGuestCount roomStayGuestCount1 = new RoomStayGuestCount()
      roomStayGuestCountList.add( roomStayGuestCount1 )
      roomStayGuestCount1.ageQualifier = "8"
      roomStayGuestCount1.count = "1"
      RoomStayGuestCount roomStayGuestCount2 = new RoomStayGuestCount()
      roomStayGuestCountList.add( roomStayGuestCount2 )
      roomStayGuestCount2.ageQualifier = "10"
      roomStayGuestCount2.count = "2"

      RoomStayTimeSpan roomStayTimeSpan = new RoomStayTimeSpan()
      roomStay.timeSpan = roomStayTimeSpan
      roomStayTimeSpan.duration = "P3D"
      roomStayTimeSpan.start = "2012-08-15-12:00"
      roomStayTimeSpan.end = "2012-08-18-12:00"

      List<RoomAmenity> roomAmenities = new ArrayList<RoomAmenity>()
      roomStay.amenitiesList = roomAmenities
      RoomAmenity roomAmenity1 = new RoomAmenity()
      roomAmenities.add( roomAmenity1 )
      roomAmenity1.id = "Complimentary Breakfast"
      roomAmenity1.needConfirmation = "true"
      roomAmenity1.description = "Complimentary continental breakfast served between 06:30 and 10:00 daily"

      RoomAmenity roomAmenity2 = new RoomAmenity()
      roomAmenities.add( roomAmenity2 )
      roomAmenity2.id = "Dinner"
      roomAmenity2.includedInRate = "false"
      roomAmenity2.needConfirmation = "false"
      roomAmenity2.description = "Dinner at extra cost"

      PropertyInfo propertyInfo = new PropertyInfo()
      roomStay.basicPropertyInfo = propertyInfo
      propertyInfo.chainCode = requestObjJSON.requestData.search.criteriaList[0].groupCode
      propertyInfo.hotelCode = requestObjJSON.requestData.search.criteriaList[0].itemCode
      propertyInfo.name = "Holiday Inn"
      propertyInfo.city = "My City"
      propertyInfo.state = "GA"
      propertyInfo.country = "USA"

      List<HotelAmenity> hotelAmenityList = new ArrayList<HotelAmenity>()
      propertyInfo.hotelAmenitiesList = hotelAmenityList
      HotelAmenity hotelAmenity1 = new HotelAmenity()
      hotelAmenityList.add(hotelAmenity1)
      hotelAmenity1.id = "159"
      hotelAmenity1.includedInRate = "true"
      hotelAmenity1.confirmable = "true"
      hotelAmenity1.description = "Complimentary continental breakfast served between 6:30 and 10:00 daily"
      HotelAmenity hotelAmenity2 = new HotelAmenity()
      hotelAmenityList.add(hotelAmenity2)
      hotelAmenity2.id = "175"
      hotelAmenity2.includedInRate = "false"
      hotelAmenity2.confirmable = "false"
      hotelAmenity2.description = "No such thing as free lunch sucker"

      JsonIOUtil.writeTo( writer, availResponse, availResponse.cachedSchema(), false );
    }
    catch ( Throwable e )
    {
      return this.logAndBuildErrorResponse( requestObjJSON,
        "RezTripSim - Couldn't create an OPS JSON response to be sent back", e, OpsErrorCode.SERVICE_RESPONSE_FORMAT_ERROR )
    }

    sLogger.info( "Successfully created the OPS JSON response to be sent back ..." )
    return writer.toString()
  }

  /**
   * Populate room stay rate data in the given RoomStay object
   * <p />
   * @param roomStay - object to populate
   */
  def private populateRoomStayRates( RoomStay roomStay )
  {
    List<RoomStayRate> roomStayRateList = new ArrayList<RoomStayRate>()
    roomStay.roomStayRatesList = roomStayRateList

    RoomStayRate roomStayRate = new RoomStayRate()
    roomStayRateList.add(roomStayRate)
    roomStayRate.availabilityStatus = "AvailableForSale"
    roomStayRate.roomTypeCode = "A1K"
    roomStayRate.description = "King room corporate rate"
    roomStayRate.numberOfUnits = "1"
    roomStayRate.ratePlanCode = "P123"
    roomStayRate.bookingCode = "A1KP123"

    // 1st Rate
    List<RoomRate> roomRateList = new ArrayList<RoomRate>()
    roomStayRate.ratesList = roomRateList
    RoomRate roomRate = new RoomRate()
    roomRateList.add( roomRate)
    roomRate.rateTimeOfUnit = "Day"
    roomRate.minLOS = "2"
    roomRate.maxLOS = "7"
    roomRate.rateMode = "4"

    BaseRoomStayRate baseRoomStayRate = new BaseRoomStayRate()
    roomRate.baseRate = baseRoomStayRate
    baseRoomStayRate.amountBeforeTax = "120.00"
    baseRoomStayRate.decimalPlaces = "2"
    baseRoomStayRate.amountAfterTax = "135.00"
    baseRoomStayRate.currencyCode = "USD"

    List<RoomStayAdditionalGuestFee> roomStayRateAdditionalGuestFeeList = new ArrayList<RoomStayAdditionalGuestFee>()
    roomRate.additionalGuestFeesList = roomStayRateAdditionalGuestFeeList
    RoomStayAdditionalGuestFee roomStayRateAdditionalGuestFee = new RoomStayAdditionalGuestFee()
    roomStayRateAdditionalGuestFeeList.add( roomStayRateAdditionalGuestFee )
    roomStayRateAdditionalGuestFee.id = "Child"
    roomStayRateAdditionalGuestFee.ageQualifier = "8"
    roomStayRateAdditionalGuestFee.amountBeforeTax = "20.00"
    roomStayRateAdditionalGuestFee.amountAfterTax = "28.79"
    roomStayRateAdditionalGuestFee.decimalPlaces = "2"
    roomStayRateAdditionalGuestFee.currencyCode = "USD"

    List<RoomStayAdditionalFee> roomStayRateAdditionalFeesList = new ArrayList<RoomStayAdditionalFee>()
    roomRate.additionalFeesList = roomStayRateAdditionalFeesList
    RoomStayAdditionalFee roomStayRateAdditionalFee = new RoomStayAdditionalFee()
    roomStayRateAdditionalFeesList.add( roomStayRateAdditionalFee )
    roomStayRateAdditionalFee.id = "Rollaway Fee"
    roomStayRateAdditionalFee.amount = "20.00"
    roomStayRateAdditionalFee.currencyCode = "USD"

    List<RoomStayCancelPenalty> roomStayRateCancelPenaltyList = new ArrayList<RoomStayCancelPenalty>()
    roomRate.cancelPenaltiesList = roomStayRateCancelPenaltyList
    RoomStayCancelPenalty roomStayRateCancelPenalty = new RoomStayCancelPenalty()
    roomStayRateCancelPenaltyList.add( roomStayRateCancelPenalty )
    roomStayRateCancelPenalty.id = "BeforeArrival"
    roomStayRateCancelPenalty.description = "Cancel penalty is 50 percentage of total price if cancelled within 2 days of arrival"
    roomStayRateCancelPenalty.timeUnit = "Day"
    roomStayRateCancelPenalty.multiplier = "2"
    roomStayRateCancelPenalty.amount = "75.00"
    roomStayRateCancelPenalty.amountPercentage = "50.00"
    roomStayRateCancelPenalty.numberOfNights = "1"
    roomStayRateCancelPenalty.basisType = "FullStay"
    roomStayRateCancelPenalty.taxInclusive = "true"
    roomStayRateCancelPenalty.feesInclusive = "true"
    RoomStayCancelPenaltyDeadline roomStayRateCancelPenaltyDeadline = new RoomStayCancelPenaltyDeadline()
    roomStayRateCancelPenalty.deadline = roomStayRateCancelPenaltyDeadline
    roomStayRateCancelPenaltyDeadline.timeUnit = "Week"
    roomStayRateCancelPenaltyDeadline.multiplier = "1"
    roomStayRateCancelPenaltyDeadline.qualifier = "BeforeArrival"

    PaymentPolicies paymentPolicies = new PaymentPolicies()
    roomRate.paymentPolicies = paymentPolicies
    RoomStayGuaranteePayment roomStayGuaranteePayment = new RoomStayGuaranteePayment()
    paymentPolicies.guaranteePayment = roomStayGuaranteePayment
    roomStayGuaranteePayment.type = "Deposit"
    roomStayGuaranteePayment.nonRefundable = "true"
    RoomStayGuaranteePaymentAmount roomStayPaymentAmt = new RoomStayGuaranteePaymentAmount()
    roomStayGuaranteePayment.amount = roomStayPaymentAmt
    roomStayPaymentAmt.value = "200.00"
    roomStayPaymentAmt.currencyCode = "USD"
    RoomStayGuaranteeDeadline deadline = new RoomStayGuaranteeDeadline()
    roomStayGuaranteePayment.deadline = deadline
    deadline.multiplier = "1"
    deadline.timeUnit = "Week"
    deadline.absoluteTime = "14:00:00"
    deadline.qualifier = "AfterBooking"
    List<AcceptablePayment> acceptablePaymentsList = new ArrayList<AcceptablePayment>()
    roomStayGuaranteePayment.acceptablePaymentsList = acceptablePaymentsList
    AcceptablePayment payment1 = new AcceptablePayment()
    acceptablePaymentsList.add( payment1 )
    payment1.id = "charge"
    payment1.type = "Visa"
    payment1.code = "VI"
    payment1.requireCVV = "true"
    AcceptablePayment payment2 = new AcceptablePayment()
    acceptablePaymentsList.add( payment2 )
    payment2.id = "charge"
    payment2.type = "Mastercard"
    payment2.code = "MC"
    payment2.requireCVV = "true"
    // End of first Rate

    // 2nd Rate
    RoomRate roomRate2 = new RoomRate()
    roomRateList.add( roomRate2)
    roomRate2.description = "Pegasus special corporare rate for deluxe king room"
    roomRate2.effectiveDate = "2011-06-01-12:00"
    roomRate2.rateTimeOfUnit = "Day"
    BaseRoomStayRate roomRate2Base = new BaseRoomStayRate()
    roomRate2.baseRate = roomRate2Base
    roomRate2Base.amountAfterTax = "120.00"
    roomRate2Base.currencyCode = "USD"
    // 3rd Rate
    RoomRate roomRate3 = new RoomRate()
    roomRateList.add( roomRate3)
    roomRate3.effectiveDate = "2011-06-05-12:00"
    roomRate3.rateTimeOfUnit = "Day"
    BaseRoomStayRate roomRate3Base = new BaseRoomStayRate()
    roomRate3.baseRate = roomRate3Base
    roomRate3Base.amountAfterTax = "90.00"
    roomRate3Base.currencyCode = "USD"

    RoomStayTotal roomStayTotal = new RoomStayTotal()
    roomStayRate.total = roomStayTotal
    roomStayTotal.amountBeforeTax = "570.00"
    roomStayTotal.amountAfterTax = "584.00"
    roomStayTotal.currencyCode = "USD"
    roomStayTotal.additionalFeesExcluded = "true"
    List<RoomStayTax> roomStayTaxesList = new ArrayList<RoomStayTax>()
    roomStayTotal.taxesList = roomStayTaxesList
    RoomStayTax tax1 = new RoomStayTax()
    roomStayTaxesList.add( tax1 )
    tax1.amount = "14.00"
    tax1.taxCode = "Federal Tax"
    tax1.percent = "5.00"
    tax1.chargeUnit = "Per Room Stay"
    RoomStayTax tax2 = new RoomStayTax()
    roomStayTaxesList.add( tax2 )
    tax2.amount = "14.00"
    tax2.taxCode = "Sales Tax"
    tax2.percent = "8.00"
    tax2.chargeUnit = "Per Room Stay"
    RoomStayTax tax3 = new RoomStayTax()
    roomStayTaxesList.add( tax3 )
    tax3.amount = "14.00"
    tax3.taxCode = "Surcharge"
    tax3.percent = "5.00"
    tax3.chargeUnit = "Per Room Stay"
  }

}
