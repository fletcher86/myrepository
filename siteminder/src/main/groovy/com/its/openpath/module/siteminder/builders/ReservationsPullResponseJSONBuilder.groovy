package com.its.openpath.module.siteminder.builders

import groovy.util.slurpersupport.GPathResult

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.dyuproject.protostuff.ByteString
import com.dyuproject.protostuff.JsonIOUtil
import com.its.openpath.module.opscommon.model.messaging.ops.ProductType
import com.its.openpath.module.opscommon.model.messaging.ops.Source
import com.its.openpath.module.opscommon.model.messaging.ops.SourceChannel
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.GuestCount
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.HotelReservation
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.Rate
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.ResGuestRPH
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.ReservationManagementType
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.ReservationPullResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.ReservationResponse
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.RoomRate
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.RoomStay
import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

@Service("ReservationsPullResponseJSONBuilder")
@ManagedResource('OPENPATH:name=/module/siteminder/builder/ReservationsPullResponseJSONBuilder')
class ReservationsPullResponseJSONBuilder
{
  private static final Logger sLogger = LoggerFactory.getLogger( ReservationsPullResponseJSONBuilder.class.name )
  
  /**
   * Constructor
   */
  ReservationsPullResponseJSONBuilder( )
  {
    sLogger.info 'Instantiated ...'
  }
  
  
  public String getReservationConfirmations(GPathResult reservationRetrieval)
  {
    sLogger.info 'GetReservationConfirmation and convert to JSON ...'
    
    String responseJson = null
    
    ReservationResponse reservationResponse = new ReservationResponse()
    reservationResponse.productType = ProductType.HOTEL_ROOM
    reservationResponse.responseData = new ReservationPullResponse()
    reservationResponse.responseData.reservationMgtType = ReservationManagementType.PULL_RESERVATIONS
    reservationResponse.responseData.extSysRefId = "n/a"
    reservationResponse.responseData.extSysTimestamp = "n/a"
    
    String correlationId = reservationRetrieval.@EchoToken
    
    ByteString correlationByteString = TimeUUIDUtils.asByteString( InvocationContext.instance.correlationId )
    
    sLogger.info("EchoToken = ${correlationId} =? InvocationContext.instance.correlationId = ${InvocationContext.instance.correlationId}")
    String timestamp = reservationRetrieval.@TimeStamp
    def success = reservationRetrieval.Success
    try
    {
      if(success)
      {
        reservationResponse.responseData.isSuccess="true"
        reservationResponse.responseData.source = new Source()
        reservationResponse.responseData.source.id = "SITEMINDER"
        reservationResponse.responseData.source.type = "22"
        reservationResponse.responseData.source.description = "Results from Siteminder regarding a reservation pull request"
        reservationResponse.responseData.source.channelInfo = new ArrayList<SourceChannel>()
        SourceChannel sc = new SourceChannel()
        
        reservationResponse.responseData.echoToken = correlationId
        reservationResponse.responseData.echoTokenBytes = correlationByteString
        reservationResponse.responseData.extSysTimestamp = timestamp
        reservationResponse.responseData.reservationList = new ArrayList<HotelReservation>()
        
        reservationRetrieval.ReservationsList.children().each
        { hotelReservation ->
          def time = hotelReservation.@CreateDateTime
          def resStatus = hotelReservation.@ResStatus
          def type = hotelReservation.UniqueID.@Type
          def id = hotelReservation.UniqueID.@ID
          
          sc.id = hotelReservation.POS.Source.RequestorId.@ID
          sc.name = hotelReservation.POS.Source.BookingChannel.CompanyName
          sc.type = hotelReservation.POS.Source.RequestorId.@Type
          reservationResponse.responseData.source.description = hotelReservation.POS.Source.BookingChannel.CompanyName.@Code
          
          sLogger.info "type = ${type}, id = ${id}, resStatus = ${resStatus}, time=${time} for correlationId = ${correlationId}"
          
          hotelReservation.ResGlobalInfo.HotelReservationIDs.children().each
          {  hotelReservationID ->
            def resIdType = hotelReservationID.@ResID_Type
            def resIdValue = hotelReservationID.@ResID_Value
            sLogger.info "resIdType = ${resIdType}, resIdValue = ${resIdValue} for correlationId = ${correlationId}"
            
            HotelReservation res = new HotelReservation()
            res.createDateTime = time
            res.resStatus = resStatus
            res.uniqueIdType = type
            res.uniqueId = id
            res.resIdType = resIdType
            res.resIdValue = resIdValue
            
            
            res.roomStayList = new ArrayList<RoomStay> ()
            
            hotelReservation.RoomStays.children().each
            { roomStay ->
              
              sLogger.info "resIdType = ${resIdType}, resIdValue = ${resIdValue} for correlationId = ${correlationId}"
              RoomStay r = new RoomStay()
              r.promotionCode = roomStay.@PromotionCode
              r.roomRate = new RoomRate()
              r.roomRate.roomTypeCode =  roomStay.RoomRates.RoomRate.@RoomTypeCode
              r.roomRate.ratePlanCode =  roomStay.RoomRates.RoomRate.@RatePlanCode
              r.roomRate.numberOfUnits =  roomStay.RoomRates.RoomRate.@NumberOfUnits
              r.roomRate.rate = new Rate()
              r.roomRate.rate.unitMultiplier = roomStay.RoomRates.RoomRate.Rates.Rate.@UnitMultiplier
              r.roomRate.rate.rateTimeUnit = roomStay.RoomRates.RoomRate.Rates.Rate.@UnitMultiplier
              r.roomRate.rate.effectiveDate = roomStay.RoomRates.RoomRate.Rates.Rate.@UnitMultiplier
              r.roomRate.rate.expireDate = roomStay.RoomRates.RoomRate.Rates.Rate.@UnitMultiplier
              r.roomRate.rate.amountAfterTax = roomStay.RoomRates.RoomRate.Rates.Rate.Base.@AmountAfterTax
              r.roomRate.rate.currencyCode = roomStay.RoomRates.RoomRate.Rates.Rate.@CurrencyCode
              r.guestCountList = new ArrayList<GuestCount>()
              roomStay.GuestCounts.children.each
              { guestCount ->
                GuestCount gc = new GuestCount()
                gc.ageQualifyingCode = guestCount.@AgeQualifyingCode
                gc.count = guestCount.@Count
                r.guestCountList.add(gc)
              }
              r.startDate = roomStay.TimeSpan.@Start
              r.endDate = roomStay.TimeSpan.@End
              r.totalAmountBeforeTax = roomStay.Total.@AmountBeforeTax
              r.totalAmountAfterTax = roomStay.Total.@AmountAfterTax
              r.totalCurrencyCode = roomStay.Total.@CurrencyCode
              r.hotelCode = roomStay.BasicPropertyInfo.@HotelCode
              roomStay.Comments.children().each
              {
                r.comment = it.Comment.Text
              }
              r.resGuestRPHList = new ArrayList<ResGuestRPH>()
              roomStay.ResGuestRPHs.children.each
              {
                ResGuestRPH rph = new ResGuestRPH()
                rph.rph = it.@RHP
                r.resGuestRPHList.add(rph)
              }
              res.roomStayList.add(r)
            }
            
            reservationResponse.responseData.reservationList.add ( res )
          }
        }
        reservationResponse.responseData.source.channelInfo.add(sc)
      }
      
      Writer writer = new StringWriter()
      JsonIOUtil.writeTo( writer, reservationResponse, reservationResponse.cachedSchema(), false )
      responseJson = writer.toString()
    }
    catch (Throwable e)
    {
      sLogger.error("Parsing error on Reservation Pull Request for correlationId=[$correlationId]", e)
    }
    return responseJson
  }
  
  /*
   * 
   *
   message GuestCount
   {
   required string ageQualifyingCode = 1 [default = "10"];
   required string count = 2;
   }
   message RoomStay
   {
   optional string promotionCode = 1;
   required RoomRate roomRate = 2;
   repeated GuestCount guestCount = 3;
   required string startDate = 4;
   required string endDate = 5;
   optional string totalAmountBeforeTax = 6;
   optional string totalAmountAfterTax = 7;
   optional string totalCurrencyCode = 8;
   required string hotelCode = 9;
   repeated ResGuestRPH resGuestRPHList = 10;
   optional string comment = 11;
   }
   message RoomRate
   {
   required string roomTypeCode = 1;
   required string numberOfUnits = 2;
   required string ratePlanCode = 3;
   required Rate rate = 4;
   }
   message Rate
   {
   required string rateTimeUnit = 1;
   required string effectiveDate = 2;
   required string expireDate = 3;
   required string unitMultiplier = 4;
   optional string currencyCode = 5;
   optional string amountAfterTax = 6;
   optional string amountBeforeTax = 7;
   }
   */
}
