package com.its.openpath.module.reztripsim.gds.reservation

import com.dyuproject.protostuff.JsonIOUtil
import com.its.openpath.module.opscommon.model.messaging.ops.ErrorMessage
import com.its.openpath.module.opscommon.model.messaging.ops.ErrorResponse
import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode
import com.its.openpath.module.opscommon.model.messaging.ops.ProductType
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.AlternateCalendarAvailabilityResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.AlternatePropertyInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.AlternatePropertyResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.AlternateRoomInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.AlternateRoomsOrPackagesResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.CalendarAvailabilityInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ExtraChargeInformation
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.GuaranteeDepositAndCancelPolicyInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RateChargeInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ReservationManagementType
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ReservationRequest
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ReservationResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomCancellationInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomReservationCancellationResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomReservationConfirmationResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomReservationIgnoreOrEndResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomReservationManagementRequest
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomReservationManagementResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomReservationRequest
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomReservationUnavailableResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomStayConfirmation
import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.opscommon.util.TimeUUIDUtils
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedOperation
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response

/**
 * <code>GDSReservationManagementResource</code>
 * <p/>
 * The RESTful Resource that handles Reservation Management tasks.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service("GDSReservationManagementResource")
@ManagedResource('OPENPATH:name=/module/reztripsim/gds/reservation/GDSReservationManagementResource')
@Path("/reservation")
class GDSReservationManagementResource
{
  private static final Logger sLogger = LoggerFactory.getLogger( GDSReservationManagementResource.class.name )


  private DateTimeFormatter mDateTimeFormatter = DateTimeFormat.forPattern( "ddMMMyy" )
  private boolean mSendRandomReservationResponses = false


  /**
   * Constructor
   * <p />
   * @return
   */
  def GDSReservationManagementResource( )
  {
    sLogger.info "instantiated ..."
  }

  @ManagedOperation( description="Send random Unconfirmed Alternate Availablitiy responses to incoming Reservation requests")
  def sendRandomReservationResponses()
  {
    mSendRandomReservationResponses = true
  }

  /**
   * Handle Reservation Management (CREATE/UPDATE/DELETE) requests. This Resource can be accessed from:
   * <p />
   * http://[HOST_NAME]/reztripsim/rs/reservation/manage
   * <p />
   * @param servletRequest - Contains the incoming service request
   * @return Response - The Reservation Management response built
   */
  @POST
  @Path('/manage')
  @Consumes("application/json")
  @Produces("application/json")
  def Response manageReservation( @Context HttpServletRequest servletRequest )
  {
    String responseJSON = null, requestJSON = null
    Response responseObj;
    ReservationRequest requestObj = new ReservationRequest()

    // Deserialize the JSON request to its object form
    try
    {
      requestJSON = servletRequest.inputStream.text
      if ( sLogger.isDebugEnabled() )
      {
        sLogger.debug "*** REZTRIPSIM - Rcvd a Reservation Management Service Request ..."
        sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
        if ( sLogger.isDebugEnabled() )
        {
          sLogger.debug "*** REZTRIPSIM - The Reservation Management JSON message before parsing: \n[${requestJSON}]"
        }
      }
      JsonIOUtil.mergeFrom( requestJSON.bytes, requestObj, ReservationRequest.schema, false )
      UUID uuid = TimeUUIDUtils.getUniqueTimeUUIDinMicros()
      InvocationContext context = InvocationContext.getNewInstance( uuid )
      def TXN_REF = "ExtSysRef: [${requestObj.requestData.extSysRefId}]"
      context.addSessionDataItem( "TXN_REF", TXN_REF )
    }
    catch ( Throwable e )
    {
      responseJSON = this.logAndBuildErrorResponse( null,
        "REZTRIPSIM - Couldn't parse the OPS JSON request received: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}]", e, OpsErrorCode.SERVICE_REQUEST_FORMAT_ERROR )
    }

    try
    {
      if ( !responseJSON )
      {
        sLogger.debug "*** REZTRIPSIM - received Reservation Management Request: " +
          "[${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}], ${requestObj.requestData.reservationRequestList[0].reservationMgtType}"

        // Validate the new message if necessary
        responseJSON = validate( requestObj )

        // Try to create a response
        // NEW, CANCEL, IGNORE, and END Res will only have 1 RoomReservationRequest in the List, MODIFY Res will have 2 and it
        // doesn't expect a response
        if ( !responseJSON && requestObj.requestData.reservationRequestList.size() == 1 )
        {
          responseJSON = this.createOpenPathJSONResponse( requestObj )
          if ( sLogger.isDebugEnabled() )
          {
            sLogger.debug "*** REZTRIPSIM - OPS JSON Res Mgt Response for: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}] is: \n[${responseJSON}"
          }
        }
        else
        {
          if ( sLogger.isDebugEnabled() )
          {
            StringBuilder builder = new StringBuilder()
            requestObj.requestData.reservationRequestList.each {
              builder << it.reservationMgtType
              builder << ' '
            }
            sLogger.debug "*** REZTRIPSIM - *** NO *** OPS JSON Res Mgt Response to be sent back for: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}], " +
              "message types are: [${builder.toString()}]"
          }
        }
      }
    }
    catch ( Throwable e )
    {
      responseJSON = this.logAndBuildErrorResponse( null,
        "REZTRIPSIM - Couldn't build the OPS JSON response to be sent back: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}]", e, OpsErrorCode.SERVICE_REQUEST_FORMAT_ERROR )
    }

    responseObj = responseJSON ? Response.ok( responseJSON ).status( 200 ).build() : Response.ok().build()
    return responseObj
  }

  /**
   * Simple validation of the check-in and check-out dates so that a proper "Error" response can be sent back.
   * <p />
   * @param requestObjJSON - Area Pro Avl Req received
   * @return String - a JSON Error Message if the Check-in or Check-out dates are in correct; NULL if validation was successful
   */
  def private String validate( ReservationRequest requestObjJSON )
  {
    RoomReservationManagementRequest request = requestObjJSON.requestData

    request.reservationRequestList.each {
      RoomReservationRequest reservation = it
      try
      {
        // Validate only New Reservation requests
        if ( reservation.reservationMgtType != ReservationManagementType.NEW_RESERVATION )
        {
          return
        }
        if ( !reservation.checkInDate || reservation.checkInDate.isEmpty() ||
          !reservation.checkOutDate || reservation?.checkOutDate?.isEmpty() )
        {
          return this.logAndBuildErrorResponse( requestObjJSON, "Check In/Out date(s) provided are empty", null,
            OpsErrorCode.INVALID_CHECK_IN_OUT_DATE )
        }

        // YYYY/MM/DD
        DateTime startDate = mDateTimeFormatter.parseDateTime( reservation.checkInDate )
        DateTime currentDate = new DateTime()
        if ( currentDate.year != startDate.year || startDate.monthOfYear < currentDate.monthOfYear )
        {
          sLogger.info( "REZTRIPSIM - Current year: ${currentDate.year}, Check In year: ${startDate.year}, current month: ${currentDate.monthOfYear}, " +
            "Check In month: ${startDate.monthOfYear}" )
          return this.logAndBuildErrorResponse( requestObjJSON, "Check-in date ${reservation.checkInDate} provided cannot be in the past", null,
            OpsErrorCode.INVALID_CHECK_IN_OUT_DATE )
        }

        DateTime endDate = mDateTimeFormatter.parseDateTime( reservation.checkOutDate )
        if ( currentDate.year != endDate.year || endDate.monthOfYear < currentDate.monthOfYear )
        {
          sLogger.info( "REZTRIPSIM - Current year: ${currentDate.year}, Check Out year: ${endDate.year}, current month: ${currentDate.monthOfYear}, " +
            "Check Out month: ${endDate.monthOfYear}" )
          return this.logAndBuildErrorResponse( requestObjJSON, "Check-out date ${reservation.checkOutDate} provided cannot be in the past", null,
            OpsErrorCode.INVALID_CHECK_IN_OUT_DATE )
        }
      }
      catch ( Throwable e )
      {
        return this.logAndBuildErrorResponse( null,
          "REZTRIPSIM - Couldn't validate the OPS JSON request received: ${e.message}", e, OpsErrorCode.SERVICE_REQUEST_FORMAT_ERROR )
      }
    }

    return null
  }

  /**
   * Helper method to log any exception that occurred during parsing the Reservation Management Request received
   * or while building a Res Mgt Response. Creates and return a JSON Res Mgt Response with the error status set.
   * <p />
   * @param requestObj - Reservation Mgt request in Object form
   * @param errorMessage - Descriptive error message to set in the header
   * @param e - Exception caught
   * @param errorCode - Error code to set in the header
   * @return String - Reservation Mgt Response JSON stream
   */
  def String logAndBuildErrorResponse( ReservationRequest requestObj, String errorMessage, Throwable e, OpsErrorCode errorCode )
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

    ReservationResponse resMgtResponse = new ReservationResponse()
    resMgtResponse.productType = (requestObj == null) ? ProductType.UNKNOWN : requestObj.productType
    resMgtResponse.errorResponse = errorResponse
    StringWriter writer = new StringWriter()
    JsonIOUtil.writeTo( writer, resMgtResponse, resMgtResponse.cachedSchema(), false );

    writer.toString();
  }

  /**
   * Helper method to build the JSON Response to be sent back.
   * <p />
   * @param jsonRequest - Service Request received
   * @return String - The response JSON string
   */
  private String createOpenPathJSONResponse( ReservationRequest requestObjJSON )
  {
    StringWriter writer = new StringWriter()

    try
    {
      RoomReservationManagementResponse reservationMgtResponse = new RoomReservationManagementResponse()
      ReservationResponse reservationResponse = new ReservationResponse()
      reservationResponse.productType = ProductType.HOTEL_ROOM
      reservationResponse.responseData = reservationMgtResponse
      reservationMgtResponse.extSysRefId = requestObjJSON.requestData.extSysRefId
      reservationMgtResponse.extSysTimestamp = requestObjJSON.requestData.extSysTimestamp
      reservationMgtResponse.isSuccess = true

      ReservationManagementType reservationManagementType = requestObjJSON.requestData.reservationRequestList[0].reservationMgtType
      reservationMgtResponse.reservationMgtType = reservationManagementType

      if ( requestObjJSON.requestData.reservationRequestList.size() == 1 )
      {
        // List contains 1 element for NEW, CANCEL, IGNORE, and END requests
        switch ( reservationManagementType )
        {
          case ReservationManagementType.NEW_RESERVATION:
            populateNewReservationResponse( reservationMgtResponse )
            break
          case ReservationManagementType.CANCEL_RESERVATION:
            populateCancellationResponse( reservationMgtResponse )
            break
          case ReservationManagementType.IGNORE_RESERVATION:
          case ReservationManagementType.END_RESERVATION:
            populateIgnoreOrEndResponse( reservationManagementType, reservationMgtResponse )
            break
        }
      }
      else
      {
        // Modify Reservation requests can have 2 Elements in the list, one for modify and the other for delete
        // The response to send back is the same as a new Res response
        populateReservationConfirmationResponse( reservationMgtResponse )
      }

      JsonIOUtil.writeTo( writer, reservationResponse, reservationResponse.cachedSchema(), false );
    }
    catch ( Throwable e )
    {
      return this.logAndBuildErrorResponse( requestObjJSON,
        "REZTRIPSIM - Couldn't create an OPS Res Mgt JSON response to be sent back; ref: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}]", e, OpsErrorCode.SERVICE_RESPONSE_FORMAT_ERROR )
    }

    sLogger.info( "REZTRIPSIM - Successfully created the OPS Res Mgt JSON response to be sent back for: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}]" )
    return writer.toString()
  }

  /**
   * Populate the OPS JSON response to a Create New Reservation Request. The response can be either a) A Confirmation Response,
   * b) Unavailable Response, c) Cancellation Response
   * <p />
   * @param reservationMgtResponse - response to populate with the reservation confirmation and other data
   */
  def populateNewReservationResponse( RoomReservationManagementResponse reservationMgtResponse )
  {
    if ( !mSendRandomReservationResponses ) {
      populateReservationConfirmationResponse( reservationMgtResponse )
      return
    }

    switch ( new Random().nextInt( 4 ) )
    {
      case 0:
        populateReservationConfirmationResponse( reservationMgtResponse )
        break

      case 1:
        populateUnavailableResponseWithAlternateRoomsOrPackages( reservationMgtResponse )
        break

      case 2:
        populateUnavailableResponseWithAlternateCalendarAvailability( reservationMgtResponse )
        break

      case 3:
        populateUnavailableResponseWithAlternatePropertyAvailability( reservationMgtResponse )
        break
    }
  }

  /**
   * Create and populate a Confirmation Response to a Create New Reservation Request received.
   * <p />
   * @param reservationMgtResponse - response to populate with the reservation confirmation and other data
   */
  def populateReservationConfirmationResponse( RoomReservationManagementResponse reservationMgtResponse )
  {
    RoomReservationConfirmationResponse resConfirmation = new RoomReservationConfirmationResponse()
    reservationMgtResponse.reservationConfirmationResponse = resConfirmation
    resConfirmation.bookingStatus = 'HK'
    resConfirmation.confirmationNumber = '1234CONF'
    resConfirmation.roomType = '345HT' // ??
    resConfirmation.rateCategory = 'G03'
    resConfirmation.rateFrequency = 'D' // Daily
    resConfirmation.ratePlanDescription = 'Optional Rate Plan description'
    resConfirmation.bedType = 'D' // Double
    resConfirmation.roomQuality = 'A' // Deluxe
    resConfirmation.numberOfBeds = '2'
    resConfirmation.bookedRate = '350.00'
    resConfirmation.currencyCode = 'USD'
    resConfirmation.totalRoomRateNoTaxesFees = '325.00'
    resConfirmation.totalRoomRateWithTaxesFees = '350.34'
    resConfirmation.totalRoomRateWithTaxesFeesCharges = '367.77'
    resConfirmation.totalTax = '10.88'
    resConfirmation.totalSurcharges = '4.87'
    resConfirmation.guestName = 'Michael Phepps'
    resConfirmation.numberOfRooms = '1'
    resConfirmation.numberOfAdults = '2'
    resConfirmation.numberOfChildren = '1'
    resConfirmation.numberOfPersonsRate = 'double'

    RoomStayConfirmation stayConfirmation = new RoomStayConfirmation()
    resConfirmation.stayConfirmation = stayConfirmation
    stayConfirmation.credentialsRequiredAtCheckIn = 'Y'
    stayConfirmation.checkInTime = '1800'
    stayConfirmation.checkOutTime = '2330';
    stayConfirmation.checkInText = 'Bring your own booze'
    stayConfirmation.validDaysOfTheWeek = 'YNYNYNY'
    stayConfirmation.minLOS = '2'
    stayConfirmation.maxLOS = '5'
    stayConfirmation.rateChangeIndicator = 'Y'
    stayConfirmation.rateGuaranteeIndicator = 'G'
    stayConfirmation.commissionableRateStatus = 'C' // Commissionable
    stayConfirmation.paymentDeadlineTime = '1400'
    stayConfirmation.paymentDeadlineDate = '20120831' // YYYYMMDD
    stayConfirmation.paymentOffsetIndicator = 'BA'
    stayConfirmation.depositDeadline = '120D' // 120 Days
    stayConfirmation.prepaymentDeadline = '72H' // 72 Hours
    stayConfirmation.prepaymentAmount = '50.00'
    stayConfirmation.depositAmount = '23.00'
    stayConfirmation.nonRefundableStayIndicator = 'Y'
    stayConfirmation.paymentDeadline = '20120831';
    stayConfirmation.prepaymentAmountRequired = '100.00';
    stayConfirmation.depositAmountRequired = '50.00';

    RoomCancellationInfo cancellationInfo = new RoomCancellationInfo()
    resConfirmation.cancellationInfo = cancellationInfo
    cancellationInfo.cancellationDeadlineTime = '1600'
    cancellationInfo.cancellationDeadlineDate = '20120825' // YYYYMMDD
    cancellationInfo.cancellationOffsetIndicator = 'BA'
    cancellationInfo.cancellationRequiredBy = '72H' // 72 hours
    cancellationInfo.taxesIncludedInPenalty = 'Y' // Y/N/U
    cancellationInfo.feesIncludedInPenalty = 'N' // Y/N/U
    cancellationInfo.cancelPenaltyAmount = '96.00'
    cancellationInfo.cancelPenaltyNumOfNightsCharged = '1'
    cancellationInfo.cancelPenaltyPercentage = '10' // 10%
    cancellationInfo.cancellationPercentageCostQualifier = 'F' // Full Stay

    populateAdditionalElementsInNewResResponse( resConfirmation )

    resConfirmation.miscellaneousText = 'Insert miscellaneous text here'
    if ( sLogger.isDebugEnabled() )
    {
      sLogger.debug "*** REZTRIPSIM - Built a new Reservation Response for: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}]"
    }
  }

  /**
   * Populate additional info related to the reservation.
   * <p />
   * @param resConfirmation - parent element
   */
  def populateAdditionalElementsInNewResResponse( RoomReservationConfirmationResponse resConfirmation )
  {
    GuaranteeDepositAndCancelPolicyInfo guaranteeDepInfo = new GuaranteeDepositAndCancelPolicyInfo()
    resConfirmation.guaranteeDepositCancelInfo = guaranteeDepInfo
    guaranteeDepInfo.cancellationPolicy = 'Insert cancellation policy here (Char) 45'
    guaranteeDepInfo.guaranteeMethods = 'Insert guarantee method info here (Char) 45'
    guaranteeDepInfo.commissionPolicy = 'Description of Commission Policy'
    guaranteeDepInfo.cancellationPolicy = 'Amadeus only'
    guaranteeDepInfo.advancedBookingRequired = '72H'

    ExtraChargeInformation extraChargeInfo = new ExtraChargeInformation()
    resConfirmation.extraChargeInfo = extraChargeInfo
    extraChargeInfo.segmentOccurrenceCount = '1'
    extraChargeInfo.extraBedPrefixesList = ['RA', 'CR']
    extraChargeInfo.extraBedRatesList = ['25.00', '30.00']
    extraChargeInfo.extraPersonPrefixesList = ['EX', 'EC']
    extraChargeInfo.extraPersonRatesList = ['33.00', '49.99']
    extraChargeInfo.taxInfo = 'Insert Tax info here'
    extraChargeInfo.serviceCharges = 'Insert Service Charges here'

    RateChargeInfo rateChargeInfo = new RateChargeInfo()
    resConfirmation.rateChargeInfo = rateChargeInfo
    rateChargeInfo.segmentOccurrenceCount = '1'
    rateChargeInfo.rateChangeDate = '28AUG'
    rateChargeInfo.bookedRate = '238.00'
    rateChargeInfo.rateFrequency = 'W'
    rateChargeInfo.addRateChangeIndicator = 'Y'
    rateChargeInfo.extraBedPrefix = 'RA'
    rateChargeInfo.extraBedRate = '25.00'
    rateChargeInfo.extraPersonPrefix = 'EX'
    rateChargeInfo.extraPersonRate = '33.00'
  }

  /**
   * Create and populate a Unavailable Response to a Create New Reservation Request received.
   * <p />
   * @param reservationMgtResponse - response to populate with the unavailable response data
   */
  def populateUnavailableResponseWithAlternateRoomsOrPackages( RoomReservationManagementResponse reservationMgtResponse )
  {
    RoomReservationUnavailableResponse response = new RoomReservationUnavailableResponse()
    reservationMgtResponse.reservationUnavailableResponse = response

    response.bookingStatus = 'UC'
    List<ErrorMessage> errorMessageList = []
    response.errorsList = errorMessageList
    errorMessageList << new ErrorMessage( OpsErrorCode.GENERIC_INTERNAL_SERVER_ERROR, "Unable to complete the Reservation" )

    AlternateRoomsOrPackagesResponse roomsOrPackagesResponse = new AlternateRoomsOrPackagesResponse()
    response.alternateRoomsOrPackages = roomsOrPackagesResponse
    List<AlternateRoomInfo> alternateRoomInfoList = new ArrayList<AlternateRoomInfo>()
    roomsOrPackagesResponse.responseInfoList = alternateRoomInfoList
    roomsOrPackagesResponse.currencyCode = 'USD'
    roomsOrPackagesResponse.numberOfPersonsRate = '3'

    AlternateRoomInfo roomInfo = new AlternateRoomInfo()
    alternateRoomInfoList.add( roomInfo )
    roomInfo.segmentOccurrenceNumber = '1'
    roomInfo.alternateRoomRate = '110.00'
    roomInfo.alternateRoomDescription = 'PENTHOUSE,GREENS FEES,CHAMPAGNE'
    roomInfo.alternateRoomType = 'B1DD'

    roomInfo = new AlternateRoomInfo()
    alternateRoomInfoList.add( roomInfo )
    roomInfo.segmentOccurrenceNumber = '2'
    roomInfo.alternateRoomRate = '140.00'
    roomInfo.alternateRoomDescription = 'SRPVISIT USA RATE,2 QUEEN'
    roomInfo.alternateRoomType = 'VSAQQ3'

    if ( sLogger.isDebugEnabled() )
    {
      sLogger.debug "*** REZTRIPSIM - Built ALTERNATE ROOMS OR PACKAGES Response for: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}]"
    }

    //    HDR|ARS1Z|HRSXX|IAT15BF12|MSN123456789ABCDE|UTTA||BOOKRP|BSTUC||ALTRTY|SO
    //    N1|CURUSD|NPR3|ARR110.00|RTYB1DD|SRP2DOUBLE,MODERATE RATE||ALTRTY|SON2|AR
    //    R140.00|RTYVSAQQ3|SRPVISIT USA RATE,2 QUEEN||ALTRTY|SON3|ARR1000.00|RTYSU
    //    PER3|ADSPENTHOUSE,GREENS FEES,CHAMPAGNE||ALTRTY|SON4|ARR125.00|RTYA1KT|AD
    //    S1 KING, 1 TWIN,WET BAR||ALTRTY|SON5|ARR333.00|RTYPACKAGE3|ADSOCEANVIEW,B
    //    REAKFAST,GOLF,LIMO||
  }

  /**
   * Create and populate a Unavailable Response to a Create New Reservation Request received.
   * <p />
   * @param reservationMgtResponse - response to populate with the unavailable response data
   */
  def populateUnavailableResponseWithAlternateCalendarAvailability( RoomReservationManagementResponse reservationMgtResponse )
  {
    RoomReservationUnavailableResponse response = new RoomReservationUnavailableResponse()
    reservationMgtResponse.reservationUnavailableResponse = response

    response.bookingStatus = 'UC'
    List<ErrorMessage> errorMessageList = []
    response.errorsList = errorMessageList
    errorMessageList << new ErrorMessage( OpsErrorCode.GENERIC_INTERNAL_SERVER_ERROR, "Unable to complete the Reservation" )

    AlternateCalendarAvailabilityResponse alternateCalAvlRsp = new AlternateCalendarAvailabilityResponse()
    response.alternateCalendarAvailability = alternateCalAvlRsp
    alternateCalAvlRsp.currencyCode = 'USD'
    alternateCalAvlRsp.numberOfPersonsRate = '1'
    List<CalendarAvailabilityInfo> availabilityInfoList = []
    alternateCalAvlRsp.responseInfoList = availabilityInfoList

    CalendarAvailabilityInfo calendarAvlInfo = new CalendarAvailabilityInfo()
    availabilityInfoList.add( calendarAvlInfo )
    calendarAvlInfo.segmentOccurrenceNumber = '1'
    calendarAvlInfo.checkInDate = '101112'
    calendarAvlInfo.startDate = '101112'
    calendarAvlInfo.alternateRoomRate = '110.00'
    calendarAvlInfo.alternateRoomType = 'B1KD'
    calendarAvlInfo.mlosDuration = '1'
    calendarAvlInfo.ctaIndicator = 'C'
    calendarAvlInfo.availability = '3'

    calendarAvlInfo = new CalendarAvailabilityInfo()
    availabilityInfoList.add( calendarAvlInfo )
    calendarAvlInfo.segmentOccurrenceNumber = '2'
    calendarAvlInfo.alternateRoomType = 'B1KD'
    calendarAvlInfo.mlosDuration = '1'
    calendarAvlInfo.availability = '3'

    if ( sLogger.isDebugEnabled() )
    {
      sLogger.debug "*** REZTRIPSIM - Built ALTERNATE CALENDAR AVAILABILITY Response for: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}]"
    }
    //CALREP|SON3|ARR1000.00|RTYROOM TYP|MLO
    //     |MLO |MLO |MLO2|MLO |MLO |MLO |CTA |CTAC|CTAC|CTA |CTAC|CTA |CTAC|INVA|I
    //    NVA|INVA|INV-|INV-|INVA|INVA

    //    HDR|ARS1Z|HRSXX|IAT15BF12|MSN123456789ABCDE|UTTA||BOOKRP|BSTNN| ||ERRREP|
    //    SON1|DEEGUE|ERC07||CALREP|SON1|IND31DEC96|SDT29DEC89|CURUSD|NPR1|ARR110.0
    //    0|RTYB1KD|MLO |MLO |MLO |MLO2|MLO |MLO |MLO |CTA |CTA |CTA |CTA |CTAC|CTA
    //     |CTA |INV2|INV3|INV-|INVA|INV5|INV-|INV-||CALREP|SON2|ARR140.00|RTYVSAQQ
    //    3|MLO |MLO |MLO |MLO2|MLO |MLO |MLO |CTA |CTA |CTA |CTA |CTAC|CTA |CTA |I
    //    NV1|INV2|INV1|INV3|INV3|INV-|INV-||CALREP|SON3|ARR1000.00|RTYROOM TYP|MLO
    //     |MLO |MLO |MLO2|MLO |MLO |MLO |CTA |CTAC|CTAC|CTA |CTAC|CTA |CTAC|INVA|I
    //    NVA|INVA|INV-|INV-|INVA|INVA||CALREP|SON4|ARR65.00|RTYC1D|MLO |MLO |MLO3|
    //    MLO |MLO |MLO |MLO |CTA |CTA |CTA |CTAC|CTAC|CTA |CTA |INVA|INV7|INV5|INV
    //    5|INV5|INV1|INV7||
  }

  /**
   * Create and populate a Unavailable Response to a Create New Reservation Request received.
   * <p />
   * @param reservationMgtResponse - response to populate with the unavailable response data
   */
  def populateUnavailableResponseWithAlternatePropertyAvailability( RoomReservationManagementResponse reservationMgtResponse )
  {
    RoomReservationUnavailableResponse response = new RoomReservationUnavailableResponse()
    reservationMgtResponse.reservationUnavailableResponse = response

    response.bookingStatus = 'UC'
    List<ErrorMessage> errorMessageList = []
    response.errorsList = errorMessageList
    errorMessageList << new ErrorMessage( OpsErrorCode.GENERIC_INTERNAL_SERVER_ERROR, "Unable to complete the Reservation" )

    AlternatePropertyResponse alternatePropertyRsp = new AlternatePropertyResponse()
    response.alternateProperties = alternatePropertyRsp
    List<AlternatePropertyInfo> alternatePropertyInfoList = []
    alternatePropertyRsp.responseInfoList = alternatePropertyInfoList

    alternatePropertyRsp.notAvailablePropertyId = 'PHX002'
    alternatePropertyRsp.numberOfPersonsRate = '1'

    AlternatePropertyInfo propertyInfo = new AlternatePropertyInfo()
    alternatePropertyInfoList.add( propertyInfo )
    propertyInfo.segmentOccurrenceNumber = '1'
    propertyInfo.distanceFromProperty = '10'
    propertyInfo.distanceUnits = 'MI'
    propertyInfo.directionFromProperty = 'NE'
    propertyInfo.alternatePropertyName = 'INN COSTA LOTS'
    propertyInfo.alternatePropertyLocation = '134, Hotel Lane'
    propertyInfo.alternatePropertyId = 'NOGL02'
    propertyInfo.alternateRoomRate = '245.99'
    propertyInfo.alternateRoomType = 'SUITE'
    propertyInfo.currencyCode = 'USD'

    propertyInfo = new AlternatePropertyInfo()
    alternatePropertyInfoList.add( propertyInfo )
    propertyInfo.segmentOccurrenceNumber = '1'
    propertyInfo.alternatePropertyName = 'INN COSTA LOTS'
    propertyInfo.alternatePropertyId = 'NOGL02'
    propertyInfo.alternateRoomRate = '245.99'
    propertyInfo.alternateRoomType = 'SUITE'
    propertyInfo.currencyCode = 'USD'

    if ( sLogger.isDebugEnabled() )
    {
      sLogger.debug "*** REZTRIPSIM - Built ALTERNATE PROPERTY AVAILABILITY Response for: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}]"
    }
    //    HDR|ARS1Z|HRSXX|IAT15BF12|MSN123456789ABCDE|UTTA||BOOKRP|BSTUC|CNF4676467
    //    |NAMHURDES/GEORGEMR||ERRREP|SON1|DEEGUE|ERC07||ALTRTY|SON1|CURUSD|NPR3|AR
    //    R110.00|RTYB1DD|SRP2DOUBLE,MODERATE RATE||ALTPID|SON1|PIDPHX002|NPR1|DIP9
    //    9|DUNKM|DFPSE|APNINN COSTA LOTS|APINOGL02|ARR99927500.|CURMEP|RTYSUITE||C
    //    ALREP|SON1|IND31DEC96|SDT29DEC89|CURUSD|NPR1|ARR110.00|RTYB1KD|MLO |MLO |
    //    MLO |MLO2|MLO |MLO |MLO |CTA |CTA |CTA |CTA |CTAC|CTA |CTA |INV2|INV3|INV
    //    -|INVA|INV5|INV-|INV-||BLABLA|TXTTHIS IS ONE LINE OF MISCELLANEOUS TEXT.|
    //    |
  }

  /**
   * Populate the OPS JSON response to a Reservation Cancellation Request.
   * <p />
   * @param reservationMgtResponse - response to populate with the reservation confirmation and other data
   */
  def populateCancellationResponse( RoomReservationManagementResponse reservationMgtResponse )
  {
    RoomReservationCancellationResponse cancellationResponse = new RoomReservationCancellationResponse()
    reservationMgtResponse.reservationCancellationResponse = cancellationResponse

    cancellationResponse.bookingStatus = 'XK'
    cancellationResponse.cancellationNumber = 'CANCEL-CON-2434-HRK'
    cancellationResponse.additionalText = 'Booooo party pooper'

    if ( sLogger.isDebugEnabled() )
    {
      sLogger.debug "*** REZTRIPSIM - Built a Reservation Cancellation Service Response for: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}]"
    }
  }

  /**
   * Populate the OPS JSON response to a Reservation Ignore or End Request.
   * <p />
   * @param reservationManagementType - type of reservation management request received -- IGNORE|END
   * @param reservationMgtResponse - response to populate with the reservation confirmation and other data
   */
  def populateIgnoreOrEndResponse( ReservationManagementType reservationManagementType, RoomReservationManagementResponse reservationMgtResponse )
  {
    RoomReservationIgnoreOrEndResponse response = new RoomReservationIgnoreOrEndResponse()
    reservationMgtResponse.reservationIgnoreOrEndResponse = response

    if ( reservationManagementType == ReservationManagementType.IGNORE_RESERVATION )
    {
      response.confirmationNumber = 'IGNORE-CON-2434-HRK'
      response.bookingStatus = 'IK'
    }
    else
    {
      response.confirmationNumber = 'ENDTXN-CON-2434-HRK'
      response.bookingStatus = 'EK'
    }

    if ( sLogger.isDebugEnabled() )
    {
      sLogger.debug "*** REZTRIPSIM - Built a Reservation: [${reservationManagementType}] Service Response for: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}]"
    }

  }

}