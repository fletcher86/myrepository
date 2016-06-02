package com.its.openpath.module.pegasus.amf.builder

import com.dyuproject.protostuff.JsonIOUtil
import com.its.openpath.module.opscommon.model.messaging.ops.ExtraBedInfo
import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode
import com.its.openpath.module.opscommon.model.messaging.ops.ProductType
import com.its.openpath.module.opscommon.model.messaging.ops.rate.GDSRatePlanInfoRequest
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateManagementType
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateRequest
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateResponse
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RoomRateManagementRequest
import com.its.openpath.module.opscommon.util.InvocationContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import com.its.openpath.module.opscommon.model.messaging.ops.rate.GDSRatePlanInfoResponse

/**
 * <code>AMFRateManagementMessageBuilder</code>
 * <p/>
 * The {@link com.its.openpath.module.pegasus.amf.handler.AMFRateManagementHandler} invokes this Builder
 * to build a JSON Rate Management Service Request Messages to be sent to OpenPath and build AMF messages to be
 * sent to Pegasus USW based the Service Response Message received from OpenPath.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service('AMFRateManagementMessageBuilder')
class AMFRateManagementMessageBuilder
extends AbstractAMFBuilder
{
  private static Logger sLogger = LoggerFactory.getLogger( AMFRateManagementMessageBuilder.class.name )

  /**
   * @see {@link com.its.openpath.module.pegasus.AbstractBaseBuilder#buildRequestToOpenPathFromExternal}
   */
  def boolean buildRequestToOpenPathFromExternal( )
  {
    InvocationContext context = InvocationContext.instance
    AMFRequestBodyElementMap bodyElementMap = new AMFRequestBodyElementMap()

    try
    {
      String messageBody = context.externalSystemRequestData
      // Skip the message header and start by getting the first segment -- RPINRQ
      int segmentBeginIdx = messageBody.indexOf( 'RPINRQ|' ) + 'RPINRQ'.length() + 1
      int segmentEndIdx = messageBody.indexOf( '||', segmentBeginIdx )

      ExtraBedInfo extraBedInfo = null
      List<String> childAgeList
      List<String> specialServiceReqList
      childAgeList = []
      bodyElementMap.addChildAgeList( childAgeList )

      // Get the data that constitutes the RPINRQ segment
      def segmentData = messageBody.substring( segmentBeginIdx, segmentEndIdx )

      // Parse individual elements in the segment delimited by the '|' char
      def bodyElements = segmentData.split( "\\|" )

      // Iterate over each element found in the current segment and and put their names and values in to the Map
      bodyElements.each { String element ->
        if ( element.startsWith( '\n' ) )
        {
          element = element.substring( 1, element.length() )
        }

        // Element name is the first 3 chars, the rest is the data, e.g GMT051756
        String elementName = element.substring( 0, 3 )
        Object elementData = element.substring( 3, element.length() )

        // Handle Extra Bed info (can occour 3 times) and put the resulting List<ExtraBedInd> in the body map
        if ( elementName.equals( 'EBP' ) && elementData )
        {
          if ( extraBedInfo )
          {
            bodyElementMap.addExtraBedInfo( extraBedInfo )
          }
          extraBedInfo = new ExtraBedInfo()
        }
        if ( elementName.equals( 'EBP' ) || elementName.equals( 'EBN' ) )
        {
          populateExtraBedInfoInRequestMessage( elementName, elementData, extraBedInfo )
          return
        }
        else if ( extraBedInfo )
        {
          bodyElementMap.addExtraBedInfo( extraBedInfo )
          extraBedInfo = null
        }

        // Handle child ages, can occour 99 times!
        if ( elementName.equals( 'AGE' ) )
        {
          childAgeList << elementData
          return
        }

        // If the element doesn't need to be handled in a special way as above, put its name + value in the Map
        bodyElementMap.addField( elementName, elementData )
      } // End of clousure processing each element found in the segment

      if ( extraBedInfo )
      {
        bodyElementMap.addExtraBedInfo( extraBedInfo )
      }

      if ( sLogger.debugEnabled )
      {
        sLogger.debug "PEGASUS - AMF Message Body rcvd for: [${context.getSessionDataItem( "TXN_REF" )}] is:\n ${bodyElementMap}"
      }
      context.openPathRequestData = this.buildRatePlanInfoRequestToOPS( bodyElementMap )
    }
    catch ( Throwable e )
    {
      def errMsg = "PEGASUS - Couldn't build a Rate Plan Info Service Request to be sent to OpenPath: [${context.getSessionDataItem( "TXN_REF" )}]"
      sLogger.error errMsg, e
      buildErrorResponse( OpsErrorCode.SERVICE_REQUEST_PROCESSING_ERROR, errMsg )
      return false
    }

    if ( sLogger.debugEnabled )
    {
      sLogger.debug "PEGASUS - Built OPS Rate Plan Info Req for: [${context.getSessionDataItem( "TXN_REF" )}]"
    }

    return true
  }

  /**
   * Populate the given object that represents extra bed info with the matching data elements.
   * <p />
   * @param elementName - name of the data element received in the request data
   * @param elementData - value of the data element received in the request data
   * @param extraBedInfo - object to populate
   */
  def private populateExtraBedInfoInRequestMessage( String elementName, String elementData, ExtraBedInfo extraBedInfo )
  {
    if ( elementName.equals( 'EBN' ) )
    {
      extraBedInfo.quantity = elementData
    }
    if ( elementName.equals( 'EBP' ) )
    {
      extraBedInfo.prefix = elementData
    }
  }


  /**
   * Build the JSON JSON Rate Plan Info Request to be submitted to the OPS Message Bus.
   * <p />
   * @param messageBodyElementMap - contains the data element names and their values received in the AMF Service Request from USW
   * @return String - JSON message built
   */
  def private String buildRatePlanInfoRequestToOPS( AMFRequestBodyElementMap messageBodyElementMap )
  {
    InvocationContext context = InvocationContext.instance
    Map<String, String> headerValuesMap = (Map<String, String>) context.getSessionDataItem( "REQ_HEADER" )
    RateRequest rateRequest = new RateRequest()
    RoomRateManagementRequest roomRateReq = new RoomRateManagementRequest()
    GDSRatePlanInfoRequest ratePlanInfoReq = new GDSRatePlanInfoRequest()
    rateRequest.productType = ProductType.HOTEL_ROOM
    rateRequest.requestData = roomRateReq
    roomRateReq.gdsRatePlanInfo = ratePlanInfoReq

    RateManagementType type = (RateManagementType) context.getSessionDataItem( 'MESSAGE_TYPE' )
    roomRateReq.rateManagementType = type
    roomRateReq.extSysRefId = "${headerValuesMap.get( 'MSN' )}"
    roomRateReq.extSysTimestamp = new Date().time

    ratePlanInfoReq.chainCode = messageBodyElementMap.getField( 'CHN' )
    ratePlanInfoReq.hotelCode = messageBodyElementMap.getField( 'PID' )
    ratePlanInfoReq.hotelCity = messageBodyElementMap.getField( 'HCY' )
    ratePlanInfoReq.pseudoCityCode = messageBodyElementMap.getField( 'PCC' )
    ratePlanInfoReq.checkInDate = messageBodyElementMap.getField( 'IND' )
    ratePlanInfoReq.checkOutDate = messageBodyElementMap.getField( 'OTD' )
    ratePlanInfoReq.lengthOfStay = messageBodyElementMap.getField( 'NNT' )
    ratePlanInfoReq.ratePlanCode = messageBodyElementMap.getField( 'RPC' )
    ratePlanInfoReq.ratePlanIndicator = messageBodyElementMap.getField( 'RPI' )
    ratePlanInfoReq.rateCategory = messageBodyElementMap.getField( 'RCY' )
    ratePlanInfoReq.corporateAccountNumber = messageBodyElementMap.getField( 'CCN' )
    ratePlanInfoReq.frequentGuestNumber = messageBodyElementMap.getField( 'CUA' )
    ratePlanInfoReq.requestedCurrencyCode = messageBodyElementMap.getField( 'RCU' )
    ratePlanInfoReq.nativeCurrencyCode = messageBodyElementMap.getField( 'NCU' )
    ratePlanInfoReq.confirmationNumber = messageBodyElementMap.getField( 'CNF' )
    ratePlanInfoReq.roomTypeCode = messageBodyElementMap.getField( 'RTC' )
    ratePlanInfoReq.roomOccupancyTotal = messageBodyElementMap.getField( 'ROC' )
    ratePlanInfoReq.numberOfPayingPersonsPerRoom = messageBodyElementMap.getField( 'NPR' )
    ratePlanInfoReq.numberOfChildren = messageBodyElementMap.getField( 'NCH' )
    ratePlanInfoReq.ageOfChildrenList = messageBodyElementMap.getChildAgeList()
    ratePlanInfoReq.numberOfBeds = messageBodyElementMap.getField( 'NBD' )
    ratePlanInfoReq.bedType = messageBodyElementMap.getField( 'BDT' )
    ratePlanInfoReq.roomQuality = messageBodyElementMap.getField( 'RMQ' )
    ratePlanInfoReq.extraBedsList = messageBodyElementMap.getExtraBedInfoList()
    ratePlanInfoReq.numberOfRooms = messageBodyElementMap.getField( 'NRM' )
    ratePlanInfoReq.calculatePercentages = messageBodyElementMap.getField( 'CPI' )
    ratePlanInfoReq.taxApplication = messageBodyElementMap.getField( 'TAI' )

    super.removeEmptyListsAndFieldsFromRequest( rateRequest )

    StringWriter writer = new StringWriter()
    JsonIOUtil.writeTo( writer, rateRequest, rateRequest.getSchema(), false );
    return writer.toString()
  }

  /**
   *********************************************************************************************************************
   *********************************************************************************************************************
   * All functions that deal with creating the USW response starts from here
   *********************************************************************************************************************
   *********************************************************************************************************************
   */

  /**
   * @see {@link com.its.openpath.module.pegasus.AbstractBaseBuilder#buildResponseToExternalFromOpenPath}
   */
  def boolean buildResponseToExternalFromOpenPath( )
  {
    InvocationContext context = InvocationContext.instance
    RateResponse rateResponse = new RateResponse()

    try
    {
      JsonIOUtil.mergeFrom( ((String) context.openPathResponseData).bytes, rateResponse, rateResponse.schema, false )
    }
    catch ( Throwable e )
    {
      def errMsg = "PEGASUS - Couldn't deserialize the JSON Rate Management Response received from OpenPath, ${context.getSessionDataItem( "TXN_REF" )}"
      sLogger.error errMsg, e
      this.buildErrorResponse( OpsErrorCode.SERVICE_RESPONSE_PROCESSING_ERROR, errMsg )
      return false
    }

    if ( rateResponse.errorResponse )
    {
      List<AMFErrorMessage> amfErrorMessageList = new ArrayList<AMFErrorMessage>( rateResponse.errorResponse.errorMessagesList.size() )
      rateResponse.errorResponse.errorMessagesList.each {
        AMFErrorMessage amfErrorMessage = new AMFErrorMessage( code: it.errorCode.number, optionalText: it.errorMessage )
        amfErrorMessageList.add( amfErrorMessage )
      }
      this.buildAMFErrorResponse( amfErrorMessageList, rateResponse.errorResponse.additionalText)
      return true
    }

    switch ( rateResponse.responseData.rateManagementType )
    {
      case RateManagementType.RATE_PLAN_INFO:
        this.populateRatePlanInfoResponseMessage( rateResponse )
        break
    }

    return true
  }

  /**
   * Build the Response message for a Rate Plan Info request received. The response will be
   * set it in the InvocationContext and will be sent to the Pegasus USW by the 'handler' instance.
   * <p />
   * @param rateResponse - JSON response received from the OpenPath Messge Bus
   */
  def populateRatePlanInfoResponseMessage( RateResponse rateResponse )
  {
    InvocationContext context = InvocationContext.instance
    context.success = false
    def writer = new StringBuilder()

    try
    {

      writer << getSoapCrapBegin()
      writer << (String) InvocationContext.instance.getSessionDataItem( "MESSAGE_HDR" )
      writer << '||RPINRP'

      GDSRatePlanInfoResponse response = rateResponse.responseData.gdsRatePlanInfo
      appendField( '|CHN', response.chainCode, writer )
      appendField( '|PID', response.hotelCode, writer )
      appendField( '|PCC', response.pseudoCityCode, writer )
      appendField( '|IND', response.checkInDate, writer )
      appendField( '|OTD', response.checkOutDate, writer )
      appendField( '|RPC', response.ratePlanCode, writer )
      appendField( '|RPI', response.ratePlanIndicator, writer )
      appendField( '|RPN', response.ratePlanName, writer )
      appendField( '|CCN', response.corporateAccountNumber, writer )
      appendField( '|CUA', response.frequentGuestNumber, writer )
      appendField( '|CRD', response.credentialsRequiredIndicator, writer )
      appendField( '|NCU', response.nativeCurrencyCode, writer )
      appendField( '|VSD', response.validStartDate, writer )
      appendField( '|VED', response.validEndDate, writer )
      appendField( '|MLO', response.minimumLOS, writer )
      appendField( '|XLO', response.maximumLOS, writer )
      appendField( '|TX1', response.taxes1, writer )
      appendField( '|TX2', response.taxes2, writer )
      appendField( '|TX3', response.taxes3, writer )
      appendField( '|SUR', response.surcharges, writer )
      appendField( '|SVC', response.serviceCharges, writer )
      appendField( '|SR1', response.specialRequirements1, writer )
      appendField( '|SR2', response.specialRequirements2, writer )
      appendField( '|SR3', response.specialRequirements3, writer )
      appendField( '|ABR', response.advancedBookingRequired, writer )
      appendField( '|GTR', response.guaranteeRequired, writer )
      appendField( '|GTM', response.guaranteeMethods, writer )
      appendField( '|DPR', response.depositRequired, writer )
      appendField( '|DPB', response.depositRequiredBy, writer )
      appendField( '|DPM', response.depositMethods, writer )
      appendField( '|PPR', response.prepaidRequired, writer )
      appendField( '|PPB', response.prepaidRequiredBy, writer )
      appendField( '|PPM', response.prepaidMethods, writer )
      appendField( '|DPP', response.depositPolicy, writer )
      appendField( '|PDT', response.paymentDeadlineTime, writer )
      appendField( '|PDD', response.paymentDeadlineDate, writer )
      appendField( '|PDI', response.paymentOffsetIndicator, writer )
      appendField( '|NFS', response.nonRefundableStayIndicator, writer )
      appendField( '|CMP', response.commissionPolicy, writer )
      appendField( '|CX1', response.cancellationPolicyLine1, writer )
      appendField( '|CX2', response.cancellationPolicyLine2, writer )
      appendField( '|CDT', response.cancellationDeadlineTime, writer )
      appendField( '|CDD', response.cancellationDeadlineDate, writer )
      appendField( '|CDI', response.cancellationOffsetIndicator, writer )
      appendField( '|CPA', response.cancelPenaltyAmount, writer )
      appendField( '|CPT', response.taxesIncludedInCancelPenaltyAmt = 'Y', writer )
      appendField( '|CPF', response.feesIncludedInCancelPenaltyAmt, writer )
      appendField( '|CPN', response.cancelPenaltyNumOfNightsCharged, writer )
      appendField( '|CPP', response.cancelPenaltyPercentage, writer )
      appendField( '|CPQ', response.cancellationPercentageCostQualifier, writer )
      appendField( '|RP1', response.ratePlanText1, writer )
      appendField( '|RP2', response.ratePlanText2, writer )
      appendField( '|RP3', response.ratePlanText3, writer )
      appendField( '|RTC', response.roomTypeCode, writer )
      appendField( '|RPD', response.ratePlanDescription, writer )
      appendField( '|PKG', response.packageOptions, writer )
      appendField( '|MKT', response.marketingMessage, writer )
      appendField( '|CMR', response.commissionableRateStatus, writer )
      appendField( '|ABD', response.advanceBookingDescription, writer )
      appendField( '|MLD', response.minimumStayPolicy, writer )
      appendField( '|XLD', response.maximumStayPolicy, writer )
      response.structuredGuaranteeMethodsList.each { appendField( '|GMD', it, writer )}
      response.structuredDepositMethodsList.each { appendField( '|DMD', it, writer )}
      appendField( '|CID', response.checkInDescription, writer )
      appendField( '|COD', response.checkOutDescription, writer )
      appendField( '|XPI', response.expressCheckInDescription, writer )
      appendField( '|XPO', response.expressCheckOutDescription, writer )
      writer << '||'

      writer << getSoapCrapEnd()
      if ( sLogger.isDebugEnabled() )
      {
        sLogger.debug "PEGASUS - built a: [${rateResponse.responseData.rateManagementType}] " +
          "response to be sent to Pegasus: [${context.getSessionDataItem( "TXN_REF" )}]"
      }
    }
    catch ( Throwable e )
    {
      def errMsg = "PEGASUS - Couldn't build a: [${rateResponse.responseData.rateManagementType}] response to be sent to Pegasus: [${context.getSessionDataItem( "TXN_REF" )}]"
      sLogger.error errMsg, e
      this.buildErrorResponse( OpsErrorCode.SERVICE_RESPONSE_PROCESSING_ERROR, errMsg )
    }
    context.externalSystemResponseData = writer.toString()
  }


}
