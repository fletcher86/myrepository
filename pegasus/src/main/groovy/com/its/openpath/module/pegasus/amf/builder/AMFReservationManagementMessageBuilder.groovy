package com.its.openpath.module.pegasus.amf.builder

import com.dyuproject.protostuff.JsonIOUtil
import com.its.openpath.module.opscommon.model.messaging.ops.Address
import com.its.openpath.module.opscommon.model.messaging.ops.AddressType
import com.its.openpath.module.opscommon.model.messaging.ops.Email
import com.its.openpath.module.opscommon.model.messaging.ops.EmailType
import com.its.openpath.module.opscommon.model.messaging.ops.ErrorMessage
import com.its.openpath.module.opscommon.model.messaging.ops.GuestInfo
import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode
import com.its.openpath.module.opscommon.model.messaging.ops.Phone
import com.its.openpath.module.opscommon.model.messaging.ops.PhoneType
import com.its.openpath.module.opscommon.model.messaging.ops.ProductType
import com.its.openpath.module.opscommon.model.messaging.ops.PropertyInfo
import com.its.openpath.module.opscommon.model.messaging.ops.RoomInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.AlternateCalendarAvailabilityResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.AlternatePropertyInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.AlternatePropertyResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.AlternateRoomInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.AlternateRoomsOrPackagesResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ArrivalInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.CalendarAvailabilityInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.CreditCardPaymentInfo
import com.its.openpath.module.opscommon.model.messaging.ops.ExtraBedInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ExtraChargeInformation
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ExtraPersonInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.GuaranteeDepositAndCancelPolicyInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.GuaranteeInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.GuestDataSegment
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RateChargeInfo
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ReservationManagementType
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ReservationRequest
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ReservationResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomReservationCancellationResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomReservationConfirmationResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomReservationIgnoreOrEndResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomReservationManagementRequest
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomReservationRequest
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.RoomReservationUnavailableResponse
import com.its.openpath.module.opscommon.util.InvocationContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import com.its.openpath.module.opscommon.model.messaging.ops.Source

/**
 * <code>AMFReservationManagementMessageBuilder</code>
 * <p/>
 * The {@link com.its.openpath.module.pegasus.amf.handler.AMFReservationManagementHandler} invokes this Builder
 * to build a JSON Reservation Management Service Request Messages to be sent to OpenPath and build AMF messages to be
 * sent to Pegasus USW based the Service Response Message received from OpenPath.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service('AMFReservationManagementMessageBuilder')
class AMFReservationManagementMessageBuilder
extends AbstractAMFBuilder
{
  private static Logger sLogger = LoggerFactory.getLogger( AMFReservationManagementMessageBuilder.class.name )


  /**
   * @see {@link com.its.openpath.module.pegasus.AbstractBaseBuilder#buildRequestToOpenPathFromExternal}
   */
  def boolean buildRequestToOpenPathFromExternal( )
  {
    InvocationContext context = InvocationContext.instance
    Map<ReservationManagementType, AMFRequestBodyElementMap> messageBodyElementMap = new LinkedHashMap<ReservationManagementType, AMFRequestBodyElementMap>()
    AMFRequestBodyElementMap bodyElementMap

    try
    {
      ReservationManagementType reservationMsgType = (ReservationManagementType) context.getSessionDataItem( "MESSAGE_TYPE" )
      String messageBody = context.externalSystemRequestData
      // Skip the message header and start by getting the first segment -- BOOKRQ
      int segmentBeginIdx = messageBody.indexOf( 'BOOKRQ|' ) + 'BOOKRQ'.length() + 1
      int segmentEndIdx = messageBody.indexOf( '||', segmentBeginIdx )

      ExtraBedInfo extraBedInfo = null
      ExtraPersonInfo extraPersonInfo = null
      List<String> mealPlanList
      List<String> childAgeList
      List<String> specialServiceReqList
      Closure initDataClosure = { ReservationManagementType type ->
        bodyElementMap = new AMFRequestBodyElementMap()
        messageBodyElementMap.put( type, bodyElementMap )
        mealPlanList = []
        bodyElementMap.addMealPlanList( mealPlanList )
        childAgeList = []
        bodyElementMap.addChildAgeList( childAgeList )
        specialServiceReqList = []
        bodyElementMap.addSpecialReqList( specialServiceReqList )
      }
      initDataClosure( reservationMsgType )

      // Iterate all segments (e.g BOOKRQ, GUEST) found in the message body data
      while ( true )
      {
        // Get the data that constitutes a segment
        def segmentData = messageBody.substring( segmentBeginIdx, segmentEndIdx )

        // Parse individual elements in the segment delimited by the '|' char
        def bodyElements = segmentData.split( "\\|" )

        // Iterate over each element found in the current segment and and put their names and values in to the Map
        bodyElements.each { String element ->
          if ( element.startsWith( '\n' ) )
          {
            element = element.substring( 1, element.length() )
          }
          else if ( element.startsWith( '\r\n' ) )
          {
            element = element.substring( 2, element.length() )
          }
          if ( element.equals( 'GUEST' ) )
          {
            // Skip 'GUEST' segments, these will be handled by populateGuestDataSegmentInRequestMessage later
            return
          }
          if ( element.equals( 'DELETE' ) || element.equals( '\nDELETE' ) )
          {
            initDataClosure( ReservationManagementType.DELETE_RESERVATION )
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
          if ( extraBedInfo && elementName.equals( 'EBP' ) || elementName.equals( 'EBN' ) || elementName.equals( 'EBR' ) ||
            elementName.equals( 'BCU' ) )
          {
            populateExtraBedInfoInRequestMessage( elementName, elementData, extraBedInfo )
            return
          }
          else if ( extraBedInfo )
          {
            bodyElementMap.addExtraBedInfo( extraBedInfo )
            extraBedInfo = null
          }

          // Handle Extra Person info (can occour 2 times) and put the resulting List<ExtraBedInd> in the body map
          if ( elementName.equals( 'XPP' ) && elementData )
          {
            if ( extraPersonInfo )
            {
              bodyElementMap.addExtraPersonInfo( extraPersonInfo )
            }
            extraPersonInfo = new ExtraPersonInfo()
          }
          if ( elementName.equals( 'XPP' ) || elementName.equals( 'XPN' ) || elementName.equals( 'XPR' ) ||
            elementName.equals( 'PCU' ) )
          {
            populateExtraPersonInfoInRequestMessage( elementName, elementData, extraPersonInfo )
            return
          }
          else if ( extraPersonInfo )
          {
            bodyElementMap.addExtraPersonInfo( extraPersonInfo )
            extraPersonInfo = null
          }

          // Handle meal plans, can occour 7 times
          if ( elementName.equals( 'MPL' ) )
          {
            mealPlanList << elementData
            return
          }

          // Handle child ages, can occour 99 times!
          if ( elementName.equals( 'AGE' ) )
          {
            childAgeList << elementData
            return
          }

          // Handle special requests, can occour 5 times!
          if ( elementName.equals( 'SPR' ) )
          {
            specialServiceReqList << elementData
            return
          }

          // If the element doesn't need to be handled in a special way as above, put its name + value in the Map
          bodyElementMap.addField( elementName, elementData )
        } // End of clousure processing each element found in the current segment

        if ( extraPersonInfo )
        {
          bodyElementMap.addExtraPersonInfo( extraPersonInfo )
        }
        if ( extraBedInfo )
        {
          bodyElementMap.addExtraBedInfo( extraBedInfo )
        }

        segmentBeginIdx = segmentEndIdx + '||'.length()
        segmentEndIdx = messageBody.indexOf( '||', segmentBeginIdx )
        if ( segmentEndIdx < 0 )
        {
          // No more segments found to parse
          break
        }
      } // while loop

      if ( sLogger.debugEnabled )
      {
        sLogger.debug "PEGASUS - AMF Message Body rcvd for: [${context.getSessionDataItem( "TXN_REF" )}] is:\n ${messageBodyElementMap}"
      }
      context.openPathRequestData = this.buildRoomReservationRequestToOPS( reservationMsgType, messageBodyElementMap )
    }
    catch ( Throwable e )
    {
      def errMsg = "PEGASUS - Couldn't build a Reservation Mgt Service Request to be sent to OpenPath: [${context.getSessionDataItem( "TXN_REF" )}]"
      sLogger.error errMsg, e
      buildErrorResponse( OpsErrorCode.SERVICE_REQUEST_PROCESSING_ERROR, errMsg )
      return false
    }

    if ( sLogger.debugEnabled )
    {
      sLogger.debug "PEGASUS - Built OPS Reservation Management Req for: [${context.getSessionDataItem( "TXN_REF" )}]"
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
      return
    }
    if ( elementName.equals( 'EBP' ) )
    {
      extraBedInfo.prefix = elementData
    }
    else if ( elementName.equals( 'EBR' ) )
    {
      extraBedInfo.rate = elementData
    }
    else if ( elementName.equals( 'BCU' ) )
    {
      extraBedInfo.currencyCode = elementData
    }
  }

  /**
   * Populate the given object that represents extra person info with the matching data elements.
   * <p />
   * @param elementName - name of the data element received in the request data
   * @param elementData - value of the data element received in the request data
   * @param extraPersonInfo - object to populate
   */
  def private populateExtraPersonInfoInRequestMessage( String elementName, String elementData, ExtraPersonInfo extraPersonInfo )
  {
    if ( elementName.equals( 'XPN' ) )
    {
      return
    }
    if ( elementName.equals( 'XPP' ) )
    {
      extraPersonInfo.prefix = elementData
    }
    else if ( elementName.equals( 'XPR' ) )
    {
      extraPersonInfo.rate = elementData
    }
    else if ( elementName.equals( 'PCU' ) )
    {
      extraPersonInfo.currencyCode = elementData
    }
  }

  /**
   * Build the JSON JSON Reservation Management Request to be submitted to the OPS Message Bus.
   * <p />
   * @param reservationManagementType - CREATE|UPDATE|DELETE
   * @param messageBodyElementMap - contains the data element names and their values received in the AMF Service Request from USW
   * @return String - JSON message built
   */
  def private String buildRoomReservationRequestToOPS( ReservationManagementType reservationManagementType,
    Map<ReservationManagementType, AMFRequestBodyElementMap> messageBodyElementMap )
  {
    InvocationContext context = InvocationContext.instance
    Map<String, String> headerValuesMap = (Map<String, String>) context.getSessionDataItem( "REQ_HEADER" )
    ReservationRequest reservationRequest = new ReservationRequest()

    Source source = new Source()
    reservationRequest.source = source
    source.id = headerValuesMap.get( 'ARS' )
    source.type = 'USW'
    source.description = 'Pegasus USW'
    source.countryCode = headerValuesMap.get('SCT')
    source.pseudoCityCode = headerValuesMap.get('SCT')

    RoomReservationManagementRequest roomReservationManagementRequest = new RoomReservationManagementRequest()
    reservationRequest.productType = ProductType.HOTEL_ROOM
    reservationRequest.requestData = roomReservationManagementRequest

    roomReservationManagementRequest.reservationMgtType = reservationManagementType
    roomReservationManagementRequest.extSysRefId = "${headerValuesMap.get( 'MSN' )}"
    roomReservationManagementRequest.extSysTimestamp = new Date().time

    switch ( reservationManagementType )
    {
      case ReservationManagementType.NEW_RESERVATION:
      case ReservationManagementType.MODIFY_RESERVATION:
      case ReservationManagementType.CANCEL_RESERVATION:
      case ReservationManagementType.IGNORE_RESERVATION:
      case ReservationManagementType.END_RESERVATION:
        this.populateNewModifyCancelResMgtReqMsg( roomReservationManagementRequest,
          headerValuesMap, messageBodyElementMap )
        break
    }

    StringWriter writer = new StringWriter()
    JsonIOUtil.writeTo( writer, reservationRequest, ReservationRequest.getSchema(), false );
    return writer.toString()
  }

  /**
   * Populate data fields of the OPS JSON Reservation Management (CREATE/UPDATE/DELETE) Message with the corresponding
   * data elements received in the AMF message from Pegasus USW.
   * <p />
   * @param reservationManagementType - CREATE|UPDATE|DELETE
   * @param roomReservationManagementRequest - object to populate
   * @param headerValuesMap - contains AMF message header elements
   * @param messageBodyElementMap - contains AMF message body elements
   */
  def populateNewModifyCancelResMgtReqMsg(
    RoomReservationManagementRequest roomReservationManagementRequest,
      Map<String, String> headerValuesMap, Map<ReservationManagementType, AMFRequestBodyElementMap> messageBodyElementMap )
  {
    /**
     * ReservationRequest
     *   RoomReservationManagementRequest
     *     ReservationManagementType
     *     RoomReservation
     *       PropertyInfo
     *       GuestInfo
     *       ArrivalInfo
     *       RoomInfo
     *       ExtraBedInfo
     *       GuaranteeInfo
     *       CreditCardPaymentInfo
     *       ExtraPersonInfo
     *       GuestDataSegments
     */
    List<RoomReservationRequest> roomReservationRequestList = new ArrayList<RoomReservationRequest>()
    roomReservationManagementRequest.reservationRequestList = roomReservationRequestList

    messageBodyElementMap.each { ReservationManagementType reservationManagementType,
      AMFRequestBodyElementMap messageBodyMap ->
      RoomReservationRequest reservationReq = new RoomReservationRequest()
      roomReservationRequestList.add( reservationReq )
      reservationReq.reservationMgtType = reservationManagementType
      if ( messageBodyMap.get( 'CNF' ) )
      {
        reservationReq.confirmationNumber = messageBodyMap.get( 'CNF' )
      }
      reservationReq.bedType = messageBodyMap.getField( 'BDT' )
      reservationReq.numberOfBeds = messageBodyMap.getField( 'NBD' )
      reservationReq.checkInDate = messageBodyMap.getField( 'IND' )
      reservationReq.checkOutDate = messageBodyMap.getField( 'OTD' )
      reservationReq.mealPlansList = messageBodyMap.getMealPlanList()
      reservationReq.numberOfAdults = messageBodyMap.getField( 'NAD' )
      reservationReq.numberOfChildren = messageBodyMap.getField( 'NCH' )
      reservationReq.agesOfChildrenList = messageBodyMap.getChildAgeList()
      reservationReq.numberOfNights = messageBodyMap.getField( 'NNT' )
      reservationReq.numberOfRooms = messageBodyMap.getField( 'NRM' )
      reservationReq.specialServiceReqInfoList = messageBodyMap.getSpecialRequestsList()
      reservationReq.serviceInfo = messageBodyMap.getField( 'SIN' )
      reservationReq.tourNumber = messageBodyMap.getField( 'TNM' )

      PropertyInfo propertyInfo = new PropertyInfo()
      reservationReq.propertyInfo = propertyInfo
      propertyInfo.hotelCode = messageBodyMap.get( 'PID' )
      propertyInfo.chainCode = messageBodyMap.getField( 'CHN' )
      propertyInfo.name = messageBodyMap.getField( 'PNM' )
      propertyInfo.city = messageBodyMap.getField( 'CTY' )

      GuestInfo guestInfo = new GuestInfo()
      reservationReq.guestInfo = guestInfo
      List<String> frequentTravellerIdList = []
      guestInfo.frequencyTravellerIdsList = frequentTravellerIdList
      frequentTravellerIdList << messageBodyMap.getField( 'CUA' )
      guestInfo.name = messageBodyMap.getField( 'NAM' )
      if ( messageBodyMap.getField( 'PHN' ) )
      {
        List<Phone> phoneList = new ArrayList<Phone>()
        guestInfo.contactPhoneNumbersList = phoneList
        Phone phone = new Phone()
        phoneList << phone
        phone.type = PhoneType.OTHER
        phone.number = messageBodyMap.getField( 'PHN' )
      }
      this.populateExpandedGuestDetailsInReqMessage( messageBodyMap, guestInfo )

      ArrivalInfo arrivalInfo = new ArrivalInfo()
      reservationReq.arrivalInfo = arrivalInfo
      arrivalInfo.city = messageBodyMap.getField( "ARC" )
      arrivalInfo.time = messageBodyMap.getField( "ART" )
      arrivalInfo.flight = messageBodyMap.getField( 'FLT' )
      arrivalInfo.groupName = messageBodyMap.getField( 'GRP' )

      RoomInfo roomInfo = new RoomInfo()
      reservationReq.roomInfo = roomInfo
      roomInfo.typeId = messageBodyMap.getField( 'RTY' )
      roomInfo.quality = messageBodyMap.getField( "RMQ" )
      roomInfo.location = messageBodyMap.getField( "RML" )
      roomInfo.ratePrefix = messageBodyMap.getField( "RRP" )
      roomInfo.rate = messageBodyMap.getField( "RMR" )
      roomInfo.rateCurrencyCode = messageBodyMap.getField( "CUR" )
      roomInfo.totalRoomRate = messageBodyMap.getField( "TRI" )
      roomInfo.ratePlanCode = messageBodyMap.getField( "RPC" )

      reservationReq.extraBedsList = messageBodyMap.getExtraBedInfoList()

      GuaranteeInfo guaranteeInfo = new GuaranteeInfo()
      reservationReq.guaranteeInfo = guaranteeInfo
      guaranteeInfo.creditCardSecurityCode = messageBodyMap.getField( 'GCS' )
      guaranteeInfo.additionalInfo = messageBodyMap.getField( 'GUA' )
      guaranteeInfo.type = messageBodyMap.getField( "GUT" )
      guaranteeInfo.billingAddress = messageBodyMap.getField( 'GBA' )

      CreditCardPaymentInfo creditCardPaymentInfo = new CreditCardPaymentInfo()
      reservationReq.creditCardPaymentInfo = creditCardPaymentInfo
      creditCardPaymentInfo.typeId = messageBodyMap.getField( 'GCT' )
      creditCardPaymentInfo.number = messageBodyMap.getField( 'GCN' )
      creditCardPaymentInfo.securityCode = messageBodyMap.getField( 'GCS' )
      creditCardPaymentInfo.expiryDate = messageBodyMap.getField( 'GUE' )
      creditCardPaymentInfo.nameOnCard = messageBodyMap.getField( 'GNM' )

      reservationReq.extraPersonInfoList = messageBodyMap.getExtraPersonInfoList()
      this.populateGuestDataSegmentInReqMessage( reservationReq )

      // Remove empty Lists and objects whose all elements are null
      super.removeEmptyListsAndFieldsFromRequest( reservationReq )
    }
  }

  /**
   * Set expanded guest details if they're present in the incoming request.
   * <p />
   * @param messageBodyMap - contains the data element names and their values received in the AMF Service Request from USW
   * @param guestInfo - parent element to set data
   */
  def populateExpandedGuestDetailsInReqMessage( AMFRequestBodyElementMap messageBodyMap, GuestInfo guestInfo )
  {
    List<Phone> phoneList = new ArrayList<Phone>()
    guestInfo.contactPhoneNumbersList = phoneList
    Phone phone

    if ( messageBodyMap.get( 'PT1' ) && messageBodyMap.get( 'PH1' ) )
    {
      phone = new Phone()
      phoneList << phone
      phone.type = PhoneType.OTHER
      phone.additionalText = messageBodyMap.get( 'PT1' )
      phone.number = messageBodyMap.get( 'PH1' )
    }
    if ( messageBodyMap.get( 'PT2' ) && messageBodyMap.get( 'PH2' ) )
    {
      phone = new Phone()
      phoneList << phone
      phone.type = PhoneType.OTHER
      phone.additionalText = messageBodyMap.get( 'PT1' )
      phone.number = messageBodyMap.get( 'PH1' )
    }
    if ( messageBodyMap.get( 'PT3' ) && messageBodyMap.get( 'PH3' ) )
    {
      phone = new Phone()
      phoneList << phone
      phone.type = PhoneType.OTHER
      phone.additionalText = messageBodyMap.get( 'PT3' )
      phone.number = messageBodyMap.get( 'PH3' )
    }
    if ( messageBodyMap.get( 'PT4' ) && messageBodyMap.get( 'PH4' ) )
    {
      phone = new Phone()
      phoneList << phone
      phone.type = PhoneType.OTHER
      phone.additionalText = messageBodyMap.get( 'PT4' )
      phone.number = messageBodyMap.get( 'PH4' )
    }

    List<Address> addressList = new ArrayList<Address>()
    guestInfo.addressesList = addressList
    Address address
    if ( messageBodyMap.getField( 'T1A' ) && messageBodyMap.getField( 'AD1' ) )
    {
      address = new Address()
      addressList << address
      address.type = AddressType.OTHER
      address.additionalText = messageBodyMap.getField( 'T1A' )
      address.line1 = messageBodyMap.getField( 'AD1' )
    }
    if ( messageBodyMap.getField( 'T2A' ) && messageBodyMap.getField( 'AD2' ) )
    {
      address = new Address()
      addressList << address
      address.type = AddressType.OTHER
      address.additionalText = messageBodyMap.getField( 'T2A' )
      address.line1 = messageBodyMap.getField( 'AD2' )
    }
    if ( messageBodyMap.getField( 'T3A' ) && messageBodyMap.getField( 'AD3' ) )
    {
      address = new Address()
      addressList << address
      address.type = AddressType.OTHER
      address.additionalText = messageBodyMap.getField( 'T3A' )
      address.line1 = messageBodyMap.getField( 'AD3' )
    }

    List<Email> emailList = new ArrayList<Email>()
    guestInfo.contactEmailsList = emailList
    Email email
    if ( messageBodyMap.getField( 'ET1' ) && messageBodyMap.get( 'EM1' ) )
    {
      email = new Email()
      emailList << email
      email.type = EmailType.OTHER
      email.additionalText = messageBodyMap.get( 'ET1' )
      email.address = messageBodyMap.get( 'EM1' )
    }
    if ( messageBodyMap.getField( 'ET2' ) && messageBodyMap.get( 'EM2' ) )
    {
      email = new Email()
      emailList << email
      email.type = EmailType.OTHER
      email.additionalText = messageBodyMap.get( 'ET2' )
      email.address = messageBodyMap.get( 'EM2' )
    }
  }

  /**
   * Populate additional guest data known as the 'Guest Data Segment' present in the 'GUEST' segment.
   * <p />
   * @param reservationReq - Request data to read the GUEST segment data
   */
  def populateGuestDataSegmentInReqMessage( RoomReservationRequest reservationReq )
  {
    String allGuestData = InvocationContext.instance.externalSystemRequestData

    int guestDataBeginIdx = allGuestData.indexOf( 'GUEST|' )
    if ( guestDataBeginIdx < 0 )
    {
      // Quit if no 'GUEST' data segment are present
      return
    }

    List<GuestDataSegment> guestDataSegmentList = new ArrayList<GuestDataSegment>()
    reservationReq.guestDataSegmentsList = guestDataSegmentList
    // Get the reference to the first 'GUEST' data segment
    int guestDataEndIdx = allGuestData.indexOf( '||', guestDataBeginIdx )

    // Iterate over all GUEST data segments and add data elements in them to the List of GuestDataSegment objects
    while ( true )
    {
      String oneGuestData = allGuestData.substring( guestDataBeginIdx, guestDataEndIdx )
      def guestDataElements = oneGuestData.split( "\\|" )

      GuestDataSegment guestDataSegment = new GuestDataSegment()
      guestDataSegmentList.add( guestDataSegment )

      guestDataElements.each { String element ->
        // Element name is the first 3 chars, the rest is the data -- GMT051756
        String elementName = element.substring( 0, 3 )
        Object elementData = element.substring( 3, element.length() )
        if ( elementName.equals( 'NAD' ) )
        {
          guestDataSegment.numberOfAdults = elementData
        }
        if ( elementName.equals( 'NAM' ) )
        {
          guestDataSegment.guestName = elementData
        }
        if ( elementName.equals( 'NCH' ) )
        {
          guestDataSegment.numberOfChildren = elementData
        }
        if ( elementName.equals( 'EAD' ) )
        {
          guestDataSegment.email = new Email()
          guestDataSegment.email.type = EmailType.OTHER
          guestDataSegment.email.address = elementData
        }
        if ( elementName.equals( 'PHN' ) )
        {
          guestDataSegment.phone = new Phone()
          guestDataSegment.phone.type = PhoneType.OTHER
          guestDataSegment.phone.number = elementData
        }
        if ( elementName.equals( 'AD1' ) )
        {
          guestDataSegment.address = new Address()
          guestDataSegment.address.type = AddressType.OTHER
          guestDataSegment.address.line1 = elementData
        }
      }

      guestDataBeginIdx = allGuestData.indexOf( 'GUEST|', guestDataEndIdx )
      if ( guestDataBeginIdx < 0 )
      {
        // Quit if there's no more GUEST data segments found
        break
      }
      guestDataEndIdx = allGuestData.indexOf( '||', guestDataBeginIdx )
    }
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
    ReservationResponse reservationResponse = new ReservationResponse()

    try
    {
      JsonIOUtil.mergeFrom( ((String) context.openPathResponseData).bytes,
        reservationResponse, ReservationResponse.schema, false )
    }
    catch ( Throwable e )
    {
      def errMsg = "PEGASUS - Couldn't deserialize the JSON Reservation Management Response received from OpenPath, ${context.getSessionDataItem( "TXN_REF" )}"
      sLogger.error errMsg, e
      this.buildErrorResponse( OpsErrorCode.SERVICE_RESPONSE_PROCESSING_ERROR, errMsg )
      return false
    }

    if ( reservationResponse.errorResponse )
    {
      List<AMFErrorMessage> amfErrorMessageList = new ArrayList<AMFErrorMessage>( reservationResponse.errorResponse.errorMessagesList.size() )
      reservationResponse.errorResponse.errorMessagesList.each {
        AMFErrorMessage amfErrorMessage = new AMFErrorMessage( code: it.errorCode.number, optionalText: it.errorMessage )
        amfErrorMessageList.add( amfErrorMessage )
      }
      this.buildAMFErrorResponse( amfErrorMessageList, reservationResponse.errorResponse.additionalText )
      return true
    }

    this.populateReservationManagementRspMessage( reservationResponse )
    return true
  }

  /**
   * Build the Response message for a Reservation Management request received. The response will be
   * set it in the InvocationContext and will be sent to the Pegasus USW by the 'handler' instance.
   * <p />
   * @param resMgtResponse - JSON response received from the OpenPath Messge Bus
   */
  def populateReservationManagementRspMessage( ReservationResponse resMgtResponse )
  {
    InvocationContext context = InvocationContext.instance
    context.success = false
    def writer = new StringBuilder()

    try
    {
      writer << getSoapCrapBegin()
      writer << (String) InvocationContext.instance.getSessionDataItem( "MESSAGE_HDR" )
      writer << '||BOOKRP'

      // Populate the New Reservation response
      def newReservationClosure = {
        RoomReservationConfirmationResponse confirmation = resMgtResponse.responseData.reservationConfirmationResponse
        appendField '|BST', confirmation.bookingStatus, writer
        appendField '|CNF', confirmation.confirmationNumber, writer
        appendField '|RTY', confirmation.roomType, writer
        appendField '|RCY', confirmation.rateCategory, writer
        appendField '|RFQ', confirmation.rateFrequency, writer
        appendField '|RPD', confirmation.ratePlanDescription, writer
        appendField '|BDT', confirmation.bedType, writer
        appendField '|RMQ', confirmation.roomQuality, writer
        appendField '|NBD', confirmation.numberOfBeds, writer
        appendField '|BKR', confirmation.bookedRate, writer
        appendField '|CUR', confirmation.currencyCode, writer
        appendField '|TRR', confirmation.totalRoomRateNoTaxesFees, writer
        appendField '|TRI', confirmation.totalRoomRateWithTaxesFees, writer
        appendField '|TRE', confirmation.totalRoomRateWithTaxesFeesCharges, writer
        appendField '|TTX', confirmation.totalTax, writer
        appendField '|TSC', confirmation.totalSurcharges, writer
        appendField '|NAM', confirmation.guestName, writer
        appendField '|NRM', confirmation.numberOfRooms, writer
        appendField '|NAD', confirmation.numberOfAdults, writer
        appendField '|NCH', confirmation.numberOfChildren, writer
        appendField '|CRD', confirmation.stayConfirmation.credentialsRequiredAtCheckIn, writer
        appendField '|CKI', confirmation.stayConfirmation.checkInTime, writer
        appendField '|CKO', confirmation.stayConfirmation.checkOutTime, writer
        appendField '|CKX', confirmation.stayConfirmation.checkInText, writer
        appendField '|VDW', confirmation.stayConfirmation.validDaysOfTheWeek, writer
        appendField '|MLO', confirmation.stayConfirmation.minLOS, writer
        appendField '|XLO', confirmation.stayConfirmation.maxLOS, writer
        appendField '|RCI', confirmation.stayConfirmation.rateChangeIndicator, writer
        appendField '|GQI', confirmation.stayConfirmation.rateGuaranteeIndicator, writer
        appendField '|CMR', confirmation.stayConfirmation.commissionableRateStatus, writer
        appendField '|PDT', confirmation.stayConfirmation.paymentDeadlineTime, writer
        appendField '|PDD', confirmation.stayConfirmation.paymentDeadlineDate, writer
        appendField '|PDI', confirmation.stayConfirmation.paymentOffsetIndicator, writer
        appendField '|DPR', confirmation.stayConfirmation.depositDeadline, writer
        appendField '|PPR', confirmation.stayConfirmation.prepaymentDeadline, writer
        appendField '|PPA', confirmation.stayConfirmation.prepaymentAmountRequired, writer
        appendField '|DPA', confirmation.stayConfirmation.depositAmountRequired, writer
        appendField '|NFS', confirmation.stayConfirmation.nonRefundableStayIndicator, writer
        appendField '|CDT', confirmation.cancellationInfo.cancellationDeadlineTime, writer
        appendField '|CDD', confirmation.cancellationInfo.cancellationDeadlineDate, writer
        appendField '|CDI', confirmation.cancellationInfo.cancellationOffsetIndicator, writer
        appendField '|CXT', confirmation.cancellationInfo.cancellationRequiredBy, writer
        appendField '|CPT', confirmation.cancellationInfo.taxesIncludedInPenalty, writer
        appendField '|CPF', confirmation.cancellationInfo.feesIncludedInPenalty, writer
        appendField '|CPA', confirmation.cancellationInfo.cancelPenaltyAmount, writer
        appendField '|CPN', confirmation.cancellationInfo.cancelPenaltyNumOfNightsCharged, writer
        appendField '|CPP', confirmation.cancellationInfo.cancelPenaltyPercentage, writer
        appendField '|CPQ', confirmation.cancellationInfo.cancellationPercentageCostQualifier, writer
        writer << '||'

        populateAdditionalElementsInResponseMessage( confirmation, writer )
      }

      // Populate the Reservation Cancellation response
      def cancelReservationClosure = {
        RoomReservationCancellationResponse cancellation = resMgtResponse.responseData.reservationCancellationResponse
        appendField '|BST', cancellation.bookingStatus, writer
        appendField '|CAN', cancellation.cancellationNumber, writer
        writer << '||'
        writer << 'BLABLA'
        appendField '|TXT', cancellation.additionalText, writer
        writer << '||'
      }

      // Populate the Reservation Ignore or End response
      def ignoreOrEndReservationClosure = {
        RoomReservationIgnoreOrEndResponse ignoreOrEndResponse = resMgtResponse.responseData.reservationIgnoreOrEndResponse
        appendField '|BST', ignoreOrEndResponse.bookingStatus, writer
        appendField '|CNF', ignoreOrEndResponse.confirmationNumber, writer
        writer << '||'
      }

      def unavailableBookingResponseClosure = {
        RoomReservationUnavailableResponse unavailableResponse = resMgtResponse.responseData.reservationUnavailableResponse
        AlternateRoomsOrPackagesResponse alternateRoomsOrPackagesRsp = unavailableResponse?.alternateRoomsOrPackages
        AlternateCalendarAvailabilityResponse alternateCalendarAvlRsp = unavailableResponse?.alternateCalendarAvailability
        AlternatePropertyResponse alternatePropertyRsp = unavailableResponse?.alternateProperties
        List<ErrorMessage> errorMessageList = unavailableResponse?.errorsList

        appendField '|BST', unavailableResponse.bookingStatus, writer
        if ( errorMessageList && !errorMessageList.empty )
        {
          errorMessageList.each { ErrorMessage errorMessage ->
            writer << '||ERRREP'
            writer << '|ERC' << mErrorCodeMap.getAMFErrorCodeMapping( errorMessage.errorCode.number )
            writer << '|ETX' << errorMessage.errorMessage
          }
        }

        if ( alternateRoomsOrPackagesRsp )
        {
          writer << '||ALTRTY'
          appendField '|CUR', alternateRoomsOrPackagesRsp.currencyCode, writer
          appendField '|NPR', alternateRoomsOrPackagesRsp.numberOfPersonsRate, writer
          alternateRoomsOrPackagesRsp.responseInfoList.each { AlternateRoomInfo alternateRoomInfo ->
            appendField '|SON', alternateRoomInfo.segmentOccurrenceNumber, writer
            appendField '|APR', alternateRoomInfo.alternateRoomRate, writer
            appendField '|ADS', alternateRoomInfo.alternateRoomDescription, writer
            appendField '|RTY', alternateRoomInfo.alternateRoomType, writer
          }
          writer << '||'
          if ( sLogger.isDebugEnabled() )
          {
            sLogger.debug "PEGASUS - built a: [${resMgtResponse.responseData.reservationMgtType}] - ALTERNATE ROOMS OR PACKAGES AVL " +
              "response to be sent to Pegasus: [${context.getSessionDataItem( "TXN_REF" )}]"
          }
        }
        else if ( alternateCalendarAvlRsp )
        {
          writer << '||CALREP'
          appendField '|CUR', alternateCalendarAvlRsp.currencyCode, writer
          appendField '|NPR', alternateCalendarAvlRsp.numberOfPersonsRate, writer
          alternateCalendarAvlRsp.responseInfoList.each { CalendarAvailabilityInfo calAvlInfo ->
            appendField '|SON', calAvlInfo.segmentOccurrenceNumber, writer
            appendField '|IND', calAvlInfo.checkInDate, writer
            appendField '|SDT', calAvlInfo.startDate, writer
            appendField '|APR', calAvlInfo.alternateRoomRate, writer
            appendField '|RTY', calAvlInfo.alternateRoomType, writer
            appendField '|MLO', calAvlInfo.mlosDuration, writer
            appendField '|CTA', calAvlInfo.ctaIndicator, writer
            appendField '|INV', calAvlInfo.availability, writer
          }
          writer << '||'
          if ( sLogger.isDebugEnabled() )
          {
            sLogger.debug "PEGASUS - built a: [${resMgtResponse.responseData.reservationMgtType}] - ALTERNATE CAL AVL " +
              "response to be sent to Pegasus: [${context.getSessionDataItem( "TXN_REF" )}]"
          }
        }
        else
        {
          writer << '||ALTPID'
          appendField '|PID', alternatePropertyRsp.notAvailablePropertyId, writer
          appendField '|NPR', alternatePropertyRsp.numberOfPersonsRate, writer
          alternatePropertyRsp.responseInfoList.each { AlternatePropertyInfo propertyInfo ->
            appendField '|SON', propertyInfo.segmentOccurrenceNumber, writer
            appendField '|DIP', propertyInfo.distanceFromProperty, writer
            appendField '|DUN', propertyInfo.distanceUnits, writer
            appendField '|DFP', propertyInfo.directionFromProperty, writer
            appendField '|APN', propertyInfo.alternatePropertyName, writer
            appendField '|APL', propertyInfo.alternatePropertyLocation, writer
            appendField '|API', propertyInfo.alternatePropertyId, writer
            appendField '|ARR', propertyInfo.alternateRoomRate, writer
            appendField '|CUR', propertyInfo.currencyCode, writer
            appendField '|RTY', propertyInfo.alternateRoomType, writer
          }
          writer << '||'
          if ( sLogger.isDebugEnabled() )
          {
            sLogger.debug "PEGASUS - built a: [${resMgtResponse.responseData.reservationMgtType}] - ALTERNATE PROPERTY AVL " +
              "response to be sent to Pegasus: [${context.getSessionDataItem( "TXN_REF" )}]"
          }
        }
      }

      switch ( resMgtResponse.responseData.reservationMgtType )
      {
        case ReservationManagementType.NEW_RESERVATION:
          if ( resMgtResponse.responseData.reservationConfirmationResponse )
          {
            newReservationClosure()
          }
          else
          {
            unavailableBookingResponseClosure()
          }
          break

      // No response to be sent for Modify Reservation
        case ReservationManagementType.CANCEL_RESERVATION:
          cancelReservationClosure()
          break

        case ReservationManagementType.IGNORE_RESERVATION:
        case ReservationManagementType.END_RESERVATION:
          ignoreOrEndReservationClosure()
          break
      }
      writer << getSoapCrapEnd()
    }
    catch ( Throwable e )
    {
      def errMsg = "PEGASUS - Couldn't build a: [${resMgtResponse.responseData.reservationMgtType}] response to be sent to Pegasus: [${context.getSessionDataItem( "TXN_REF" )}]"
      sLogger.error errMsg, e
      this.buildErrorResponse( OpsErrorCode.SERVICE_RESPONSE_PROCESSING_ERROR, errMsg )
    }
    context.externalSystemResponseData = writer.toString()
  }

  /**
   * Populate 'Additional Data' elements in the response to USW.
   * <p />
   * @param confirmation - reservation confirmation to read data from
   * @param writer - to append response data
   */
  def populateAdditionalElementsInResponseMessage( RoomReservationConfirmationResponse confirmation, StringBuilder writer )
  {
    GuaranteeDepositAndCancelPolicyInfo guaranteeCancelInfo = confirmation.guaranteeDepositCancelInfo
    if ( guaranteeCancelInfo )
    {
      writer << 'GUDPCN'
      writer << '|CAP' << guaranteeCancelInfo.cancellationPolicy
      writer << '|GUM' << guaranteeCancelInfo.guaranteeMethods
      writer << '|CMP' << guaranteeCancelInfo.commissionPolicy
      writer << '|ABR' << guaranteeCancelInfo.advancedBookingRequired
      writer << '||'
    }

    ExtraChargeInformation extraChargeInfo = confirmation.extraChargeInfo
    if ( extraChargeInfo )
    {
      writer << 'EXCHRG'
      writer << '|SON' << extraChargeInfo.segmentOccurrenceCount
      extraChargeInfo.extraBedPrefixesList.each {
        writer << '|EBP' << it
      }
      extraChargeInfo.extraBedRatesList.each {
        writer << '|EBR' << it
      }
      extraChargeInfo.extraPersonPrefixesList.each {
        writer << '|XPP' << it
      }
      extraChargeInfo.extraPersonRatesList.each {
        writer << '|XPR' << it
      }
      writer << '|TAX' << extraChargeInfo.taxInfo
      writer << '|SVC' << extraChargeInfo.serviceCharges
      writer << '||'
    }

    RateChargeInfo rateChargeInfo = confirmation.rateChargeInfo
    if ( rateChargeInfo )
    {
      writer << 'RATCHG'
      writer << '|SON' << rateChargeInfo.segmentOccurrenceCount
      writer << '|RCD' << rateChargeInfo.rateChangeDate
      writer << '|BKR' << rateChargeInfo.bookedRate
      writer << '|RFQ' << rateChargeInfo.rateFrequency
      writer << '|RCI' << rateChargeInfo.addRateChangeIndicator
      writer << '|EBP' << rateChargeInfo.extraBedPrefix
      writer << '|EBR' << rateChargeInfo.extraBedRate
      writer << '|XPP' << rateChargeInfo.extraPersonPrefix
      writer << '|XPR' << rateChargeInfo.extraPersonRate
      writer << '||'
    }

    if ( confirmation.miscellaneousText )
    {
      writer << 'BLABLA|TXT' << confirmation.miscellaneousText
      writer << '||'
    }
  }

}
