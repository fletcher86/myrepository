package com.its.openpath.module.reztripsim.gds.rate

import com.dyuproject.protostuff.JsonIOUtil
import com.its.openpath.module.opscommon.model.messaging.ops.ErrorMessage
import com.its.openpath.module.opscommon.model.messaging.ops.ErrorResponse
import com.its.openpath.module.opscommon.model.messaging.ops.ExtraBedInfo
import com.its.openpath.module.opscommon.model.messaging.ops.ExtraPersonInfo
import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode
import com.its.openpath.module.opscommon.model.messaging.ops.ProductType
import com.its.openpath.module.opscommon.model.messaging.ops.RoomAmenity
import com.its.openpath.module.opscommon.model.messaging.ops.rate.GDSRatePlanInfoRequest
import com.its.openpath.module.opscommon.model.messaging.ops.rate.GDSRatePlanInfoResponse
import com.its.openpath.module.opscommon.model.messaging.ops.rate.GDSRoomTypeInfoResponse
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateRequest
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateResponse
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RoomRateManagementResponse
import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.opscommon.util.TimeUUIDUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateChangeInfo

/**
 * <code>GDSRateManagementResource</code>
 * <p/>
 * The RESTful Resource that handles Rate related requests such as Rate Info, Rate Update etc.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service("GDSRateManagementResource")
@ManagedResource('OPENPATH:name=/module/reztripsim/gds/rate/GDSRateManagementResource')
@Path("/rate")
class GDSRateManagementResource
{
  private static final Logger sLogger = LoggerFactory.getLogger( GDSRateManagementResource.class.name )


  /**
   * Constructor
   * <p />
   * @return
   */
  def GDSRateManagementResource( )
  {
    sLogger.info "instantiated ..."
  }

  /**
   * Handle Rate Plan info requests. This Resource can be accessed from:
   * <p />
   * http://[HOST_NAME]/reztripsim/rs/rate/ratePlanInfo
   * <p />
   * @param servletRequest - Contains the incoming service request
   * @return Response - The Reservation Management response built
   */
  @POST
  @Path('/ratePlanInfo')
  @Consumes("application/json")
  @Produces("application/json")
  def Response manageReservation( @Context HttpServletRequest servletRequest )
  {
    String responseJSON = null, requestJSON = null
    Response responseObj;
    RateRequest requestObj = new RateRequest()

    // Deserialize the JSON request to its object form
    try
    {
      requestJSON = servletRequest.inputStream.text
      if ( sLogger.isDebugEnabled() )
      {
        sLogger.debug "*** REZTRIPSIM - Rcvd a Rate Management Service Request ..."
        sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
        if ( sLogger.isDebugEnabled() )
        {
          sLogger.debug "*** REZTRIPSIM - The Rate Management JSON message before parsing: \n[${requestJSON}]"
        }
      }
      JsonIOUtil.mergeFrom( requestJSON.bytes, requestObj, RateRequest.schema, false )
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
        sLogger.debug "*** REZTRIPSIM - received Rate Management Request: " +
          "[${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}], ${requestObj.requestData.rateManagementType}"
        responseJSON = this.createOpenPathJSONResponse( requestObj )
        if ( sLogger.isDebugEnabled() )
        {
          sLogger.debug "*** REZTRIPSIM - OPS JSON Rate Mgt Response for: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}] is: \n[${responseJSON}"
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
   * Helper method to log any exception that occurred during processing of Rate Management Requests received
   * or while building a Rate Mgt Response. Creates and return a JSON Rate Mgt Response with the error status set.
   * <p />
   * @param requestObj - Rate Mgt request in Object form
   * @param errorMessage - Descriptive error message to set in the header
   * @param e - Exception caught
   * @param errorCode - Error code to set in the header
   * @return String - Rate Mgt Response JSON stream
   */
  def String logAndBuildErrorResponse( RateRequest requestObj, String errorMessage, Throwable e, OpsErrorCode errorCode )
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

    RateResponse rspObject = new RateResponse()
    rspObject.productType = requestObj ? requestObj.productType : ProductType.UNKNOWN
    rspObject.errorResponse = errorResponse
    StringWriter writer = new StringWriter()
    JsonIOUtil.writeTo( writer, rspObject, rspObject.cachedSchema(), false );

    writer.toString();
  }

  /**
   * Helper method to build the JSON Response to be sent back.
   * <p />
   * @param jsonRequest - Service Request received
   * @return String - The response JSON string
   */
  private String createOpenPathJSONResponse( RateRequest requestObjJSON )
  {
    StringWriter writer = new StringWriter()

    try
    {
      RateResponse rateResponse = new RateResponse()
      rateResponse.productType = requestObjJSON.productType
      RoomRateManagementResponse roomRateManagementResponse = new RoomRateManagementResponse()
      roomRateManagementResponse.rateManagementType = requestObjJSON.requestData.rateManagementType
      rateResponse.responseData = roomRateManagementResponse
      GDSRatePlanInfoResponse gdsRatePlanInfoResponse = new GDSRatePlanInfoResponse()
      roomRateManagementResponse.gdsRatePlanInfo = gdsRatePlanInfoResponse

      roomRateManagementResponse.extSysRefId = requestObjJSON.requestData.extSysRefId
      roomRateManagementResponse.extSysTimestamp = requestObjJSON.requestData.extSysTimestamp
      roomRateManagementResponse.isSuccess = true

      GDSRatePlanInfoRequest request = requestObjJSON.requestData.gdsRatePlanInfo
      gdsRatePlanInfoResponse.chainCode = request.chainCode
      gdsRatePlanInfoResponse.hotelCode = request.hotelCode
      gdsRatePlanInfoResponse.pseudoCityCode = request.pseudoCityCode
      gdsRatePlanInfoResponse.checkInDate = request.checkInDate
      gdsRatePlanInfoResponse.checkOutDate = request.checkOutDate
      gdsRatePlanInfoResponse.ratePlanCode = request.ratePlanCode
      gdsRatePlanInfoResponse.ratePlanIndicator = "N"
      gdsRatePlanInfoResponse.ratePlanName = 'NICERP'
      gdsRatePlanInfoResponse.corporateAccountNumber = request.corporateAccountNumber
      gdsRatePlanInfoResponse.frequentGuestNumber = request.frequentGuestNumber
      gdsRatePlanInfoResponse.credentialsRequiredIndicator = 'Y'
      gdsRatePlanInfoResponse.nativeCurrencyCode = 'USD'
      gdsRatePlanInfoResponse.validStartDate = '23SEP12'
      gdsRatePlanInfoResponse.validEndDate = '22SEP13'
      gdsRatePlanInfoResponse.minimumLOS = '1'
      gdsRatePlanInfoResponse.maximumLOS = '7'
      gdsRatePlanInfoResponse.taxes1 = 'Taxes 1'
      gdsRatePlanInfoResponse.taxes2 = 'Taxes 2'
      gdsRatePlanInfoResponse.taxes3 = 'Taxes 3'
      gdsRatePlanInfoResponse.surcharges = 'Surcharges here'
      gdsRatePlanInfoResponse.serviceCharges = 'Service charges here'
      gdsRatePlanInfoResponse.specialRequirements1 = 'Special requirements 1'
      gdsRatePlanInfoResponse.specialRequirements2 = 'Special requirements 2'
      gdsRatePlanInfoResponse.specialRequirements3 = 'Special requirements 3'
      gdsRatePlanInfoResponse.advancedBookingRequired = '001D'
      gdsRatePlanInfoResponse.guaranteeRequired = '001D'
      gdsRatePlanInfoResponse.guaranteeMethods = 'Credit Card'
      gdsRatePlanInfoResponse.depositRequired = '001D'
      gdsRatePlanInfoResponse.depositRequiredBy = 'Text description of deposit, deadline, etc'
      gdsRatePlanInfoResponse.depositMethods = 'Text desc of deposit methods'
      gdsRatePlanInfoResponse.prepaidRequired = '001D'
      gdsRatePlanInfoResponse.prepaidRequiredBy = 'Text desc of prepaid deadlines etc'
      gdsRatePlanInfoResponse.prepaidMethods = 'Credit Card'
      gdsRatePlanInfoResponse.depositPolicy = 'Text desc of general deposit policy'
      gdsRatePlanInfoResponse.paymentDeadlineTime = '1300'
      gdsRatePlanInfoResponse.paymentDeadlineDate = '20120915'
      gdsRatePlanInfoResponse.paymentOffsetIndicator = 'BA'
      gdsRatePlanInfoResponse.nonRefundableStayIndicator = 'Y'
      gdsRatePlanInfoResponse.commissionPolicy = 'Text desc of commission policy'
      gdsRatePlanInfoResponse.cancellationPolicyLine1 = 'Cancel policy line1'
      gdsRatePlanInfoResponse.cancellationPolicyLine2 = 'Cancel policy line2'
      gdsRatePlanInfoResponse.cancellationDeadlineTime = '2000'
      gdsRatePlanInfoResponse.cancellationDeadlineDate = '20120913'
      gdsRatePlanInfoResponse.cancellationOffsetIndicator = 'AB'
      gdsRatePlanInfoResponse.cancelPenaltyAmount = '250.00'
      gdsRatePlanInfoResponse.taxesIncludedInCancelPenaltyAmt = 'Y'
      gdsRatePlanInfoResponse.feesIncludedInCancelPenaltyAmt = 'N'
      gdsRatePlanInfoResponse.cancelPenaltyNumOfNightsCharged = '1'
      gdsRatePlanInfoResponse.cancelPenaltyPercentage = '2'
      gdsRatePlanInfoResponse.cancellationPercentageCostQualifier = 'N'
      gdsRatePlanInfoResponse.ratePlanText1 = 'Rate Plan text 1'
      gdsRatePlanInfoResponse.ratePlanText2 = 'Rate Plan text 2'
      gdsRatePlanInfoResponse.ratePlanText3 = 'Rate Plan text 3'
      gdsRatePlanInfoResponse.roomTypeCode = 'XYZ123'
      gdsRatePlanInfoResponse.ratePlanDescription = 'Sixty four chars of Rate Plan desc'
      gdsRatePlanInfoResponse.packageOptions = 'Package options'
      gdsRatePlanInfoResponse.marketingMessage = 'Marketing message'
      gdsRatePlanInfoResponse.commissionableRateStatus = 'C'
      gdsRatePlanInfoResponse.advanceBookingDescription = 'Desc of the advanced booking policy'
      gdsRatePlanInfoResponse.minimumStayPolicy = 'Desc of min stay policy'
      gdsRatePlanInfoResponse.maximumStayPolicy = 'Desc of max stay policy'
      gdsRatePlanInfoResponse.structuredGuaranteeMethodsList = ['MC', 'VI', 'AX', 'JC']
      gdsRatePlanInfoResponse.structuredDepositMethodsList = ['MC', 'VI', 'AX', 'JC']
      gdsRatePlanInfoResponse.checkInDescription = 'Desc of the check in policy'
      gdsRatePlanInfoResponse.checkOutDescription = 'Desc of the check out policy'
      gdsRatePlanInfoResponse.expressCheckInDescription = 'Desc of the express check in policy'
      gdsRatePlanInfoResponse.expressCheckOutDescription = 'Desc of the express check out policy'

      this.populateRoomTypeInfo( gdsRatePlanInfoResponse, requestObjJSON )

      JsonIOUtil.writeTo( writer, rateResponse, rateResponse.cachedSchema(), false );
    }
    catch ( Throwable e )
    {
      return this.logAndBuildErrorResponse( requestObjJSON,
        "REZTRIPSIM - Couldn't create an OPS Rate Plan Info JSON response to be sent back; ref: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}]", e, OpsErrorCode.SERVICE_RESPONSE_FORMAT_ERROR )
    }

    sLogger.info( "REZTRIPSIM - Successfully created the OPS Rate Plan Info JSON response to be sent back for: [${InvocationContext.instance.getSessionDataItem( "TXN_REF" )}]" )
    return writer.toString()
  }

  /**
   * Populate the specific Room type info to be sent back as part of the Rate Plan Info Response.
   * <p />
   * @param response - object to populate extended data
   * @param requestObjJSON - request JSON stream received
   */
  def populateRoomTypeInfo( GDSRatePlanInfoResponse response, RateRequest requestObjJSON )
  {
    GDSRatePlanInfoRequest request = requestObjJSON.requestData.gdsRatePlanInfo
    GDSRoomTypeInfoResponse roomTypeInfo = new GDSRoomTypeInfoResponse()
    response.roomTypeInfo = roomTypeInfo

    roomTypeInfo.chainCode = request.chainCode
    roomTypeInfo.pseudoCityCode = request.pseudoCityCode
    roomTypeInfo.hotelCode = request.hotelCode
    roomTypeInfo.checkInDate = request.checkInDate
    roomTypeInfo.checkOutDate = request.checkOutDate
    roomTypeInfo.numberOfNights = '5'
    roomTypeInfo.ratePlanCode = request.ratePlanCode
    roomTypeInfo.rateChangeIndicator = 'C'
    roomTypeInfo.frequentGuestNumber = '123X4'
    roomTypeInfo.nativeCurrencyCode = 'USD'
    roomTypeInfo.requestedRateCurrencyCode = request.requestedCurrencyCode
    List<RoomAmenity> amenityList = []
    roomTypeInfo.roomAmenityList = amenityList
    RoomAmenity amenity = new RoomAmenity()
    amenity.id = '23'
    amenity.codeIdentifier = 'R'
    amenity.codeFlag = 'C'
    amenity.codeDescription = 'Mini Bar Code'
    amenityList << amenity
    amenity = new RoomAmenity()
    amenity.id = '25'
    amenity.codeIdentifier = 'B'
    amenity.codeFlag = 'E'
    amenity.codeDescription = 'Pool'
    amenityList << amenity
    roomTypeInfo.totalRoomRateExclusiveOfAllTaxesAndFees = '100.00'
    roomTypeInfo.totalRoomRateInclusiveOfAllTaxesAndFees = '125.00'
    roomTypeInfo.totalRoomRateInclusiveOfAllTaxesAndFeesAndExtraCharges = '129.99'
    roomTypeInfo.totalTax = '1.20'
    roomTypeInfo.totalSurcharges = '2.00'
    roomTypeInfo.taxQualifier = 'U'
    List<ExtraBedInfo> extraBedInfoList = []
    roomTypeInfo.extraBedInfoList = extraBedInfoList
    ExtraBedInfo extraBedInfo = new ExtraBedInfo()
    extraBedInfo.prefix = 'RA'
    extraBedInfo.rate = '25.00'
    extraBedInfoList << extraBedInfo
    roomTypeInfo.extraBedCurrency = 'USD'
    List<ExtraPersonInfo> extraPersonInfoList = []
    roomTypeInfo.extraPersonInfoList = extraPersonInfoList
    ExtraPersonInfo extraPersonInfo = new ExtraPersonInfo()
    extraPersonInfo.prefix = 'EX'
    extraPersonInfo.rate = '24.00'
    extraPersonInfoList << extraPersonInfo
    extraPersonInfo = new ExtraPersonInfo()
    extraPersonInfo.prefix = 'EC'
    extraPersonInfo.rate = '29.00'
    extraPersonInfoList << extraPersonInfo
    roomTypeInfo.extraPersonCurrency = 'USD'
    roomTypeInfo.roomTypeCode = 'YY'
    roomTypeInfo.pseudoBookingCode = '133-BOOKED'
    roomTypeInfo.roomTypeDescription1 = 'Room Type Desc1'
    roomTypeInfo.roomTypeDescription2 = 'Room Type Desc2'
    roomTypeInfo.roomTypeDescription3 = 'Room Type Desc3'
    roomTypeInfo.rateFrequency = 'D'
    roomTypeInfo.roomRateText1 = 'Room Rate Txt1'
    roomTypeInfo.roomRateText2 = 'Room Rate Txt2'
    roomTypeInfo.roomRateText3 = 'Room Rate Txt3'
    List<RateChangeInfo> rateChangeInfoList = []
    roomTypeInfo.rateChangeInfoList = rateChangeInfoList
    RateChangeInfo rateChangeInfo = new RateChangeInfo()
    rateChangeInfo.rateChangeDate = '10OCT12'
    rateChangeInfo.rateChangeRate = '5.00'
    rateChangeInfo.rateChangeText = 'Rate change'
    rateChangeInfo.rateChangeFrequency = 'D'
    rateChangeInfoList << rateChangeInfo
    rateChangeInfo = new RateChangeInfo()
    rateChangeInfo.rateChangeDate = '10OCT12'
    rateChangeInfo.rateChangeRate = '6.00'
    rateChangeInfo.rateChangeText = 'Rate change'
    rateChangeInfo.rateChangeFrequency = 'D'
    rateChangeInfoList << rateChangeInfo
    roomTypeInfo.rateChangeIndicator = 'Y'
    roomTypeInfo.bookableRoomRate = '120.00'
    roomTypeInfo.numberOfRooms = '1'
    roomTypeInfo.numberOfPayingPersonsPerRoom = '1'
    roomTypeInfo.numberOfChildren = '2'
    roomTypeInfo.roomQuality = 'Q'
    roomTypeInfo.numberOfBeds = '1'
    roomTypeInfo.bedType = 'K'
    roomTypeInfo.prepaymentAmtRequired = '99.00'
    roomTypeInfo.depositAmtRequired = '75.00'
    roomTypeInfo.flags = 'F'
    roomTypeInfo.lateArrivalTime = '5H'
    roomTypeInfo.cancellationRequired = '1D'
    roomTypeInfo.matchingQualifier = 'match?'
    roomTypeInfo.commissionAmt = '1.88'
    roomTypeInfo.commissionPercentage = '0.1'
    roomTypeInfo.maxRoomOccupancy = '2'
  }

}
