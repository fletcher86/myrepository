package com.its.openpath.module.sitemindersim.handler

import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.its.openpath.module.opscommon.util.InvocationContext



@Service
@ManagedResource('OPENPATH:name=/module/sitemindersim/handler/RateUpdateRequestHandler')
class ReservationsPullRequestHandler
{
  private static final Logger sLogger = LoggerFactory.getLogger( ReservationsPullRequestHandler.class.name )
  
  /**
   * Constructor
   */
  ReservationsPullRequestHandler( )
  {
    sLogger.info "Instantiated ..."
  }
  /**
   */
  def String process( String requestXML )
  {
    String responseXML = null
    InvocationContext context = InvocationContext.instance
    
    if ( sLogger.isDebugEnabled() )
    {
      sLogger.debug( "*** SITEMINDERSIM - OTA Xml Req Msg rcvd is:" )
      sLogger.debug "[${requestXML}]"
    }
    
    def request = new XmlSlurper().parseText( requestXML ).declareNamespace('SOAP-ENV': 'http://schemas.xmlsoap.org/soap/envelope/' )
    
    def body = request.Body
    
    GPathResult readRequest = body.OTA_ReadRQ
    
    String correlationId = readRequest.@EchoToken
    
    /*
     * Pick a random number between 1 and 100
     */
    Random rand = new Random()
    int randomPick = rand.nextInt( 100 )
    /*
     * Return success message if < 50
     */
    if(randomPick <= 100)
    {
      responseXML = getSuccessResponseXML(correlationId)
    }
    else
    {
      responseXML = getErrorResponseXML(correlationId)
    }
    
    //sLogger.debug "[${responseXML}]"
    return responseXML
  }
  
  /**
   * Get a simulated succesful xml response OTA_ResRetrieveRS
   *<p />
   *@return
   */
  public String getSuccessResponseXML(String correlationId)
  {
    
    Writer writer = new StringWriter()
    MarkupBuilder builder = new MarkupBuilder( writer )
    
    Closure hotelReservation = { bld, id ->
      builder.HotelReservation(CreateDateTime: new Date().toString(), ResStatus: 'Book')
      {
        UniqueID(Type: '14', ID: id)
        POS {
          Source {
            RequestorID(Type: '22', ID: 'SITEMINDER')
            BookingChannel {
              CompanyName(Code: 'WTF', 'WotIf')
            }
          }
        }
        RoomStays {
          RoomStay {
            RoomRates {
              RoomRate(RoomTypeCode: 'KB', RatePlanCode: 'RAC', NumberOfUnits: '1') {
                Rates {
                  Rate(UnitMultiplier: '3', RateTimeUnit: 'Day', EffectiveDate: '2012-12-12', ExpireDate: '2013-12-12') {
                    Base(AmountAfterTax: '100.00', CurrencyCode: 'USD')
                  }
                }
              }
            }
            GuestCounts {
              GuestCount(AgeQualifyingCode: '10', Count: '1')
            }
            TimeSpan(End: '2013-1-1', Start: '2013-1-31')
            Total(AmountAfterTax: '300.00', CurrencyCode: 'USD')
            BasicPropertyInfo(HotelCode: '10107')
            ResGuestRPHs {
              ResGuestRPH(RHP: '1')
            }
            Comments {
              Comment {
                Text('non-smoking room requested; king bed')
              }
            }
          }
        }
        ResGuests {
          ResGuest(ResGuestRPH: '1', ArrivalTime: '10:30:00') {
            Profiles {
              ProfileInfo {
                Profile(ProfileType: '1') {
                  PersonName {
                    GivenName('James')
                    SurName('Bond')
                  }
                  Telephone(PhoneNumber: '44-69-6654555')
                  Email('james.bond@mi5.co.uk')
                  Address {
                    AddressLine('Claretta House')
                    AddressLine('Tower Bridge Close')
                    CityName('London')
                    PostalCode('EC1 2PG')
                    StateProv('Middlesex')
                    CountryName('United Kingdom')
                  }
                }
              }
            }
          }
        }
        ResGlobalInfo
        {
          HotelReservationIDs
          {
            HotelReservationID (ResID_Type: "14", ResID_Value: "RES-${id}")
          }
          Total(AmountAfterTax: '300.00', CurrencyCode: 'USD')
        }
      }
    }
    
    builder.'SOAP-ENV:Envelope'( 'xmlns:SOAP-ENV': 'http://schemas.xmlsoap.org/soap/envelope/' )
    {
      builder.'SOAP-ENV:Body'()
      {
        OTA_ResRetrieveRS( xmlns: 'http://www.opentravel.org/OTA/2003/05', TimeStamp: new Date().toString(), Version: '1.0', EchoToken: correlationId)
        {
          Success()
          ReservationsList
          {
            hotelReservation.call(builder,'111222')
            hotelReservation.call(builder,'111333')
            hotelReservation.call(builder,'111444')
            hotelReservation.call(builder,'111555')
            hotelReservation.call(builder,'111666')
            hotelReservation.call(builder,'111777')
            hotelReservation.call(builder,'111888')
            hotelReservation.call(builder,'111999')
            hotelReservation.call(builder,'111101010')
          }
        }
      }
    }
    return writer.toString()
  }
  
  /**
   * Get a simulated error response xml
   *<p />
   *@return
   */
  private String getErrorResponseXML(String correlationId)
  {
    Writer writer = new StringWriter()
    MarkupBuilder builder = new MarkupBuilder( writer )
    
    builder.'SOAP-ENV:Envelope'( 'xmlns:SOAP-ENV': 'http://schemas.xmlsoap.org/soap/envelope/' )
    {
      'SOAP-ENV:Body'()
      {
        OTA_HotelAvailNotifRS( xmlns: 'http://www.opentravel.org/OTA/2003/05', TimeStamp: new Date().toString(), Version: '1.0' )
        {
          Errors
          {
            Error(Type: '5', Code: '54', 'ERROR' )
          }
        }
      }
    }
    return writer.toString()
  }
}
