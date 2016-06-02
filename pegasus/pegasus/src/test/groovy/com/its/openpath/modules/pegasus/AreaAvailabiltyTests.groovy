package com.its.openpath.modules.pegasus

import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * <code>AreaAvailabilityTests</code>
 * <p/>
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

class AreaAvailabilityTests
{
  private static Logger sLogger = LoggerFactory.getLogger( AreaAvailabilityTests.getName() );

  @Test
  def void parseRequestTest( )
  {
    def reqData = '''
<OTA_HotelAvailRQ xmlns="http://www.opentravel.org/OTA/2003/05" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" EchoToken="AB1234" TimeStamp="2011-04-01T12:31:54Z" Target="Production" Version="2.001" RateDetailsInd="true">
  <POS>
    <Source> <!-- Travel Agent's IATA -->
      <RequestorID Type="5" ID="123456" ID_Context="IATA" />
    </Source>
    <Source> <!-- Primary Channel - Alternate Distribution System (Type=2) -->
      <BookingChannel Primary="true" Type="2">
        <CompanyName Code="WB" CodeContext="USW" />
      </BookingChannel>
    </Source>
    <Source> <!-- Sub-Channel (SGA) -->
      <BookingChannel Primary="false" Type="2">
        <CompanyName Code="A1" CodeContext="USW" />
      </BookingChannel>
    </Source>
  </POS>
  <AvailRequestSegments>
    <AvailRequestSegment ResponseType="PropertyRateList">
      <StayDateRange Start="2011-06-01" End="2011-06-06" />
      <RoomStayCandidates>
        <RoomStayCandidate Quantity="1">
          <GuestCounts> <!-- Two adults -->
            <GuestCount AgeQualifyingCode="10" Count="2" />
            <!-- One child -->
            <GuestCount AgeQualifyingCode="8" Count="1" />
          </GuestCounts>
        </RoomStayCandidate>
      </RoomStayCandidates>
      <HotelSearchCriteria>
        <Criterion>
          <HotelRef HotelCode="AB1234" ChainCode="XY" HotelCodeContext="Standard" />
        </Criterion>
      </HotelSearchCriteria>
    </AvailRequestSegment>
  </AvailRequestSegments>
</OTA_HotelAvailRQ>
'''
    def OTA_HotelAvailRQ = new XmlSlurper().parseText( reqData )
//    //def firstSource = POS.Source[0]
//    println "${OTA_HotelAvailRQ.POS.Source[0].RequestorID.@Type.text()} *** "
//    OTA_HotelAvailRQ.POS.Source.each {
//      println it.BookingChannel.@Primary.text()
//    }
    println "Company Code: ${OTA_HotelAvailRQ.POS.Source[1].BookingChannel.CompanyName.@Code}, " +
      "Code Context: ${OTA_HotelAvailRQ.POS.Source[1].BookingChannel.CompanyName.@CodeContext}"

    println "Start Day: ${OTA_HotelAvailRQ.AvailRequestSegments.AvailRequestSegment.StayDateRange.@Start}, " +
      "End Day: ${OTA_HotelAvailRQ.AvailRequestSegments.AvailRequestSegment.StayDateRange.@End}, "

  }

}
