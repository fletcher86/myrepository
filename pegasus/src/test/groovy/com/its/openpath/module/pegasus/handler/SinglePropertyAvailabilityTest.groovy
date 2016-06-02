package com.its.openpath.module.pegasus.handler

import groovy.xml.MarkupBuilder
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * <code></code>
 * <p/>
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

class SinglePropertyAvailabilityTest
{
  private static Logger sLogger = LoggerFactory.getLogger( SinglePropertyAvailabilityTest.name );

  @Test
  def void createResponseToPegasus( )
  {
    try
    {
      def writer = new StringWriter()
      def xml = new MarkupBuilder( writer )
      xml.'OTA_HoteAvailRS'( xmlns: 'http://www.opentravel.org/OTA/2003/05', 'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
        EchoToken: "AB1234", TimeStamp: "20011-04-01T12:31:54Z", Target: "Production", Version: "4.001", PrimaryLangID: "en" ) {
        RoomStays() {
          RoomStay( AvailabilityStatus: "AvailableForSale", ResponseType: "PropertyRateList" )
        }
      }

      println "*** XML built: \n ${writer.toString()}"
    }
    catch ( Throwable e )
    {
      sLogger.info "Couldn't build the XML response to be sent to Pegasus, Ref: ", e
    }
  }

  private def createSampleXmlResponseToPegs( )
  {
    return """
  <OTA_HotelAvailRS xmlns="http://www.opentravel.org/OTA/2003/05" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  EchoToken="AB1234" TimeStamp="20011-04-01T12:31:54Z" Target="Production" Version="4.001" PrimaryLangID="en">
  <Success />
  <RoomStays>
    <RoomStay AvailabilityStatus="AvailableForSale" ResponseType="PropertyRateList">
      <RoomTypes>
        <RoomType RoomTypeCode="A1K" NumberOfUnits="1" NumberOfUnitsMatchType="1" RoomCategory="4" AdultOccupancyMatchType="1" ChildOccupancyMatchType="1" BedTypeMatchType="1">
          <RoomDescription>
            <Text>Deluxe king room with luxury ensuite bathroom</Text>
          </RoomDescription>
          <!-- King Bed -->
          <Amenity RoomAmenity="58" />
          <!-- Crib/cot -->
          <Amenity RoomAmenity="26" ExistsCode="1" IncludedInRateIndicator="false" ConfirmableInd="true" />
          <!-- Wireless Internet -->
          <Amenity RoomAmenity="123" IncludedInRateIndicator="false" ConfirmableInd="false">Free high speed wireless Internet access in every room</Amenity>
        </RoomType>
      </RoomTypes>
      <RatePlans>
        <!-- Requested promotional rates (PRO) are not available -->
        <RatePlan RatePlanCode="PRO" QualificationType="Requested" AvailabilityStatus="NoAvailability" />
        <!-- Requested corporate rates is available (requested "PEG" but this is mapped to rate plan "P123" in the CRS) -->
        <RatePlan RatePlanCode="P123" EffectiveDate="2010-06-01" ExpireDate="2011-12-31" RatePlanID="654321" RatePlanName="Pegasus Solutions Corporate Rate" MarketCode="C" RatePlanType="4" ID_RequiredInd="true" RatePlanCategoryMatchType="1" AvailabilityStatus="AvailableForSale">
          <RatePlanDescription>
            <Text>Corporate rate for Pegasus Solutions employees. Employee ID required at check-in</Text>
          </RatePlanDescription>
          <RatePlanInclusions TaxInclusive="false">
            <RatePlanInclusionDescription>
              <Text>Rates do not include State tax, Federal tax or any surcharges</Text>
            </RatePlanInclusionDescription>
          </RatePlanInclusions>
          <Commission StatusType="Full">
            <CommissionAmountPayable Amount="60.00" />
          </Commission>
          <AdditionalDetails>
            <!-- Requested Rate Plan Code -->
            <AdditionalDetail Type="18" Code="PEG" />
            <!-- Promotional text -->
            <AdditionalDetail Type="15">
              <DetailDescription>
                <Text>Great savings available for weekend breaks during the summer</Text>
              </DetailDescription>
            </AdditionalDetail>
          </AdditionalDetails>
        </RatePlan>
      </RatePlans>
      <RoomRates>
        <RoomRate RoomTypeCode="A1K" NumberOfUnits="1" RatePlanCode="P123" BookingCode="A1KP123">
          <Rates>
            <!-- Bookable room rate (no EffectiveDate) -->
            <Rate RateTimeUnit="Day" MinLOS="2" MaxLOS="7" RateMode="4">
              <!-- Bookable room rate (calculated as firsty night rate - RateMode="4") -->
              <Base AmountBeforeTax="120.00" CurrencyCode="USD" AdditionalFeesExcludedIndicator="true" />
              <!-- Additional guest amount are for information only - they are included in the bookable room rate -->
              <AdditionalGuestAmounts>
                <!-- Extra child charge (included in total) -->
                <AdditionalGuestAmount AgeQualifyingCode="8">
                  <Amount AmountAfterTax="20.00" CurrencyCode="USD" />
                </AdditionalGuestAmount>
              </AdditionalGuestAmounts>
              <Fees>
                <!-- Crib/cot charge (included in total) -->
                <Fee Code="37" Amount="5.00" CurrencyCode="USD" />
              </Fees>
              <CancelPolicies>
                <CancelPenalty>
                  <!-- Cancellation required 2 days before arrival to avoid penalties -->
                  <Deadline OffsetUnitMultiplier="2" OffsetTimeUnit="Day" OffsetDropTime="BeforeArrival" />
                  <!-- Cancellation penalty is 50% of full amount -->
                  <AmountPercent Percent="50.00" BasisType="FullStay" TaxInclusive="true" FeesInclusive="true" />
                  <PenaltyDescription>
                    <Text>Cancel penalty is 50 percentage of total price if cancelled within 2 days of arrival</Text>
                  </PenaltyDescription>
                </CancelPenalty>
              </CancelPolicies>
              <PaymentPolicies>
                <!-- Deposit policy -->
                <GuaranteePayment GuaranteeType="Deposit" NonRefundableIndicator="true" SeriesCodeReqInd="true">
                  <AcceptedPayments>
                    <!-- Deposit method accepted - credit card -->
                    <AcceptedPayment GuaranteeTypeCode="5" />
                    <!-- Deposit method accepted - debit card -->
                    <AcceptedPayment GuaranteeTypeCode="6" />
                    <!-- Deposit method accepted - wire payment/bank transfer -->
                    <AcceptedPayment GuaranteeTypeCode="28" />
                    <!-- American Express accepted -->
                    <AcceptedPayment PaymentTransactionTypeCode="charge">
                      <PaymentCard CardCode="AX" />
                    </AcceptedPayment>
                    <!-- MasterCard accepted -->
                    <AcceptedPayment PaymentTransactionTypeCode="charge">
                      <PaymentCard CardCode="MC" />
                    </AcceptedPayment>
                    <!-- Visa accepted -->
                    <AcceptedPayment PaymentTransactionTypeCode="charge">
                      <PaymentCard CardCode="VI" />
                    </AcceptedPayment>
                  </AcceptedPayments>
                  <!-- Deposit of \$200 required 1 week after booking -->
                  <AmountPercent Amount="200.00" CurrencyCode="USD" />
                  <Deadline OffsetUnitMultiplier="1" OffsetTimeUnit="Week" OffsetDropTime="AfterBooking" />
                </GuaranteePayment>
              </PaymentPolicies>
              <RateDescription>
                <Text>Pegasus special corporare rate for deluxe king room</Text>
              </RateDescription>
            </Rate>
            <!-- Rates Changes - (EffectiveDate present) -->
            <Rate EffectiveDate="2011-06-01" RateTimeUnit="Day">
              <Base AmountBeforeTax="120.00" CurrencyCode="USD" />
            </Rate>
            <Rate EffectiveDate="2011-06-05" RateTimeUnit="Day">
              <Base AmountBeforeTax="90.00" CurrencyCode="USD" />
            </Rate>
          </Rates>
          <RoomRateDescription>
            <Text>King room corporate rate</Text>
          </RoomRateDescription>
          <!-- Total price per room per stay (includes discounts) -->
          <Total AmountBeforeTax="570.00" AmountAfterTax="669.10" CurrencyCode="USD" AdditionalFeesExcludedIndicator="false">
            <Taxes> <!-- Federal tax -->
              <Tax Code="6" Percent="5.00" ChargeUnit="18"></Tax>
              <!-- State tax -->
              <Tax Code="15" Percent="8.00" ChargeUnit="18"></Tax>
              <!-- Service Charge -->
              <Tax Code="14" Amount="5.00" CurrencyCode="USD" ChargeUnit="18"></Tax>
            </Taxes>
          </Total>
        </RoomRate>
      </RoomRates>
      <GuestCounts>
        <!-- Rate includes 2 adults - note may not be the same as the request, e.g. if child is charged as an adult -->
        <GuestCount AgeQualifyingCode="10" Count="2" />
        <!-- Rate includes 1 child -->
        <GuestCount AgeQualifyingCode="8" Count="1" />
      </GuestCounts>
      <TimeSpan Start="2011-06-01" Duration="P5D" End="2011-06-05" />
      <BasicPropertyInfo ChainCode="XY" HotelCode="AB1234">
        <!-- Complimentary continental breakfast -->
        <HotelAmenity Code="159" IncludedInRateIndicator="true" ConfirmableInd="true">
          <Description>
            <Text>Complimentary continental breakfast served between 6:30 and 10:00 daily</Text>
          </Description>
        </HotelAmenity>
        <!-- Dinner at extra cost -->
        <HotelAmenity Code="175" IncludedInRateIndicator="false" ConfirmableInd="false" />
      </BasicPropertyInfo>
    </RoomStay>
  </RoomStays>
</OTA_HotelAvailRS>
//    """

  }

}
