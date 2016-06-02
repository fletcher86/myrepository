package com.its.openpath.modules.pegasus

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * <code></code>
 * <p/>
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

class RequestProcessorServlet
extends HttpServlet
{
  //  Logger sLogger = LoggerFactory.getLogger( "ProcessRequest" )

  @Override
  protected void doPost( final HttpServletRequest req, final HttpServletResponse resp )
  {
    def requestData = req.getReader().text

    println "*** New request rcvd: \n ${requestData}"

    def OTA_HotelAvailRQ = new XmlSlurper().parseText( requestData )

//    println "Servlet: ${OTA_HotelAvailRQ.POS.Source[0].RequestorID.@Type.text()} *** "
//    OTA_HotelAvailRQ.POS.Source.each {
//      println it.BookingChannel.@Primary.text()
//    }

    println"\n\n**********************************************"
    println "--> Company Code: ${OTA_HotelAvailRQ.POS.Source[1].BookingChannel.CompanyName.@Code}, " +
      "Code Context: ${OTA_HotelAvailRQ.POS.Source[1].BookingChannel.CompanyName.@CodeContext}"

    println "--> Start Day: ${OTA_HotelAvailRQ.AvailRequestSegments.AvailRequestSegment.StayDateRange.@Start}, " +
      "End Day: ${OTA_HotelAvailRQ.AvailRequestSegments.AvailRequestSegment.StayDateRange.@End}"
    println"**********************************************"
  }

}