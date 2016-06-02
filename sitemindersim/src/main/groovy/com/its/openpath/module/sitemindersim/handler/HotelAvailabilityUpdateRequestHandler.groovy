package com.its.openpath.module.sitemindersim.handler

import groovy.xml.MarkupBuilder

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.its.openpath.module.opscommon.util.InvocationContext



@Service
@ManagedResource('OPENPATH:name=/module/sitemindersim/handler/RateUpdateRequestHandler')
class HotelAvailabilityUpdateRequestHandler
{
  private static final Logger sLogger = LoggerFactory.getLogger( HotelAvailabilityUpdateRequestHandler.class.name )
  
  /**
   * Constructor
   */
  HotelAvailabilityUpdateRequestHandler( )
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
    
    /*
     * Pick a random number between 1 and 100
     */
    Random rand = new Random()
    int randomPick = rand.nextInt( 100 )
    /*
     * Return success message if < 50
     */
    if(randomPick <= 50)
    {
      responseXML = getSuccessResponseXML()
    }
    else
    {
      responseXML = getErrorResponseXML()
    }

    //sLogger.debug "[${responseXML}]"
    return responseXML
  }
  
  /**
   * Get a simulated succesful xml response
   *<p />
   *@return
   */
  public String getSuccessResponseXML()
  {
    Writer writer = new StringWriter()
    MarkupBuilder builder = new MarkupBuilder( writer )
    
    builder.'SOAP-ENV:Envelope'( 'xmlns:SOAP-ENV': 'http://schemas.xmlsoap.org/soap/envelope/' )
    {
      builder.'SOAP-ENV:Body'()
      {
        
        OTA_HotelAvailNotifRS( xmlns: 'http://www.opentravel.org/OTA/2003/05', TimeStamp: new Date().toString(), Version: '1.0' )
        { Success() }
      }
    }
    return writer.toString()
  }
  
  /**
   * Get a simulated error response xml
   *<p />
   *@return
   */
  private String getErrorResponseXML()
  {
    Writer writer = new StringWriter()
    MarkupBuilder builder = new MarkupBuilder( writer )
    
    builder.'SOAP-ENV:Envelope'( 'xmlns:SOAP-ENV': 'http://schemas.xmlsoap.org/soap/envelope/' )
    {
      builder.'SOAP-ENV:Body'()
      {
        OTA_HotelAvailNotifRS( xmlns: 'http://www.opentravel.org/OTA/2003/05', TimeStamp: new Date().toString(), Version: '1.0' )
        {
          Errors
          {
            Error(Type: '3', Code: '402', 'Cannon find hotelier' )
          }
        }
      }
    }
    return writer.toString()
  }
}
