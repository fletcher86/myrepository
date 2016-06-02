import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest

Logger sLogger = LoggerFactory.getLogger( "ProcessRequest" )

HttpServletRequest req = request
sLogger.info( "**** Processing request ...: ${req.getContentLength()}" )


def params = request.getParameters()
//sLogger.info( "******* ${params.dump()}")
//sLogger.info( "******* ${params.keySet()}")
//sLogger.info( "******* ${params.toStringArrayMap()}")

def response = """
<OTA_HotelAvailRS xmlns="http://www.opentravel.org/OTA/2003/05" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" EchoToken="AB1234" TimeStamp="2011-04-01T12:31:54Z" Target="Production" Version="4.001">
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
<Success />
<RoomStays> <!-- First RoomStay - King room, best available rate -->
  <RoomStay AvailabilityStatus="AvailableForSale" ResponseType="PropertyRateList">
    <RoomTypes>
      <RoomType RoomTypeCode="KNG">
        <RoomDescription>
          <Text>STANDARD KING ROOM WITH BATH AND SHOWER</Text>
        </RoomDescription>
      </RoomType>
    </RoomTypes>
    <RatePlans>
      <RatePlan RatePlanCode="BAR" AvailabilityStatus="AvailableForSale" RatePlanType="13">
        <RatePlanDescription>
          <Text>BEST AVAILABLE RATE</Text>
        </RatePlanDescription>
        <AdditionalDetails> <!-- Promotional Text (Type="15") -->
          <AdditionalDetail Type="15">
            <DetailDescription>
              <Text>JOIN OUR REWARDS PROGRAM FOR FREE NIGHTS AND MUCH MORE</Text>
            </DetailDescription>
          </AdditionalDetail>
        </AdditionalDetails>
        <Commission StatusType="Full" Percent="7.00" />
      </RatePlan>
    </RatePlans>
    <RoomRates>
      <RoomRate RoomTypeCode="KNG" RatePlanCode="BAR" AvailabilityStatus="ChangeDuringStay">
        <Rates> <!-- Bookable room rate (no EffectiveDate) -->
          <Rate RateTimeUnit="Day"> <!-- Average daily rate (RoomRate/@RateMode="3") -->
            <Base AmountBeforeTax="105.00" CurrencyCode="GBP" />
            <CancelPolicies> <!-- Must be cancelled 1 day prior to arrival to avopid penalties -->
              <CancelPenalty>
                <Deadline OffsetTimeUnit="Day" OffsetUnitMultiplier="1" OffsetDropTime="BeforeArrival" />
              </CancelPenalty>
            </CancelPolicies>
            <PaymentPolicies>
              <GuaranteePayment GuaranteeType="GuaranteeRequired" />
            </PaymentPolicies>
          </Rate>
        </Rates>
        <!-- Total price per room per stay -->
        <Total AmountBeforerTax="525.00" AmountAfterTax="630.00" CurrencyCode="GBP" />
      </RoomRate>
    </RoomRates>
    <TimeSpan Start="2011-06-01" End="2011-06-05" />
    <BasicPropertyInfo ChainCode="AB" HotelCode="1234" />
  </RoomStay>
  <!-- Second RoomStay - Twin room, best available rate -->
  <RoomStay AvailabilityStatus="AvailableForSale" ResponseType="PropertyRateList">
    <RoomTypes>
      <RoomType RoomTypeCode="TWN">
        <RoomDescription>
          <Text>STANDARD TWIN ROOM WITH BATH AND SHOWER</Text>
        </RoomDescription>
      </RoomType>
    </RoomTypes>
    <RatePlans>
      <RatePlan RatePlanCode="BAR" AvailabilityStatus="AvailableForSale" RatePlanType="13">
        <RatePlanDescription>
          <Text>BEST AVAILABLE RATE</Text>
        </RatePlanDescription>
        <AdditionalDetails> <!-- Promotional Text (Type="15") -->
          <AdditionalDetail Type="15">
            <DetailDescription>
              <Text>JOIN OUR REWARDS PROGRAM FOR FREE NIGHTS AND MUCH MORE</Text>
            </DetailDescription>
          </AdditionalDetail>
        </AdditionalDetails>
        <Commission StatusType="Full" Percent="7.00" />
      </RatePlan>
    </RatePlans>
    <RoomRates>
      <RoomRate RoomTypeCode="TWN" RatePlanCode="BAR" AvailabilityStatus="ChangeDuringStay">
        <Rates> <!-- Bookable room rate (no EffectiveDate) -->
          <Rate RateTimeUnit="Day"> <!-- Average daily rate (RoomRate/@RateMode="3") -->
            <Base AmountBeforeTax="95.00" CurrencyCode="GBP" />
            <CancelPolicies> <!-- Must be cancelled 1 day prior to arrival to avopid penalties -->
              <CancelPenalty>
                <Deadline OffsetTimeUnit="Day" OffsetUnitMultiplier="1" OffsetDropTime="BeforeArrival" />
              </CancelPenalty>
            </CancelPolicies>
            <PaymentPolicies>
              <GuaranteePayment GuaranteeType="GuaranteeRequired" />
            </PaymentPolicies>
          </Rate>
        </Rates>
        <!-- Total price per room per stay -->
        <Total AmountBeforerTax="475.00" AmountAfterTax="570.00" CurrencyCode="GBP" />
      </RoomRate>
    </RoomRates>
    <TimeSpan Start="2011-06-01" End="2011-06-06" />
    <BasicPropertyInfo ChainCode="AB" HotelCode="1234" />
  </RoomStay>
</RoomStays>
</OTA_HotelAvailRS>
"""
def key = "<OTA_HotelAvailRQ xmlns"
String requestData = params.get( key )
requestData = "${key}=${requestData}"

sLogger.info( "*** New request rcvd: \n ${requestData}" )

def OTA_HotelAvailRQ = new XmlSlurper().parseText( requestData )

sLogger.info "\n\n**********************************************"
sLogger.info "--> Company Code: ${OTA_HotelAvailRQ.POS.Source[1].BookingChannel.CompanyName.@Code}, " +
  "Code Context: ${OTA_HotelAvailRQ.POS.Source[1].BookingChannel.CompanyName.@CodeContext}"

sLogger.info "--> Start Day: ${OTA_HotelAvailRQ.AvailRequestSegments.AvailRequestSegment.StayDateRange.@Start}, " +
  "End Day: ${OTA_HotelAvailRQ.AvailRequestSegments.AvailRequestSegment.StayDateRange.@End}"
sLogger.info "**********************************************"

println(response)

//sLogger.info( "**** Processing request done ... \n ${output.toString("UTF-8")}" )
sLogger.info( "**** Processing request done ... " )
