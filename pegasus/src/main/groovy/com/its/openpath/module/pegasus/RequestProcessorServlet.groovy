package com.its.openpath.module.pegasus

import com.its.openpath.module.opscommon.model.messaging.ops.RoomAvailabilitySearchType
import com.its.openpath.module.opscommon.model.messaging.ops.rate.RateManagementType
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.ReservationManagementType
import com.its.openpath.module.pegasus.amf.handler.AMFRateManagementHandler
import com.its.openpath.module.pegasus.amf.handler.AMFReservationManagementHandler
import com.its.openpath.module.pegasus.xml.handler.XMLAreaAvailabilityRequestHandler
import com.its.openpath.module.pegasus.xml.handler.XMLSinglePropertyAvailabilityRequestHandler
import groovy.xml.MarkupBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils

import javax.servlet.ServletConfig
import javax.servlet.ServletContext
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * <code>RequestProcessorServlet</code>
 * <p/>
 * The Servlet that process all incoming Service Requests from Pegasus USW. This decides whether the received Service
 * Requests are OTA XML or AMF as well their type (Inventory Management, Reservation Management etc) and delegates processing
 * to one of the 'Handler' instances. The XML stream returned by the Handler is sent back to Pegasus USW.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */
class RequestProcessorServlet
extends HttpServlet
{
  Logger sLogger = LoggerFactory.getLogger( RequestProcessorServlet.class.name )

  private ServletContext mServletContext

  private XMLSinglePropertyAvailabilityRequestHandler mSinglePropertyAvailabilityRequestHandler
  private XMLAreaAvailabilityRequestHandler mAreaAvailabilityRequestHandler
  private AMFReservationManagementHandler mAMFReservationManagementHandler
  private AMFRateManagementHandler mAMFRateManagementHandler


  /**
   * @see {@link HttpServlet#init}
   */
  @Override
  void init( final ServletConfig config )
  {
    super.init( config )
    mServletContext = config.servletContext
    ApplicationContext ctx = WebApplicationContextUtils
      .getRequiredWebApplicationContext( mServletContext );

    mSinglePropertyAvailabilityRequestHandler = ctx.getBean( XMLSinglePropertyAvailabilityRequestHandler.class );
    mAreaAvailabilityRequestHandler = ctx.getBean( XMLAreaAvailabilityRequestHandler.class );
    mAMFReservationManagementHandler = ctx.getBean( AMFReservationManagementHandler.class )
    mAMFRateManagementHandler = ctx.getBean( AMFRateManagementHandler.class )
  }


  /**
   * @see {@link HttpServlet#doPost}
   */
  @Override
  protected void doPost( final HttpServletRequest req, final HttpServletResponse resp )
  {
    String requestData
    String responseData

    try
    {
      requestData = new String( req.getInputStream().bytes, 'US-ASCII' )
      requestData.replaceAll( '#??', '-' )
      StatisticsDashboard.requestsReceivedCount.andIncrement
      StatisticsDashboard.lastServiceRequestTimestamp.set( new Date().dateTimeString )

      if ( requestData.contains( '<OTA_HotelAvailRQ' ) && requestData.contains( 'PropertyRateList' ) )
      {
        if ( sLogger.isDebugEnabled() )
        {
          sLogger.debug "*** Rcvd a SINGLE PROPERTY AVAILABILITY REQUEST ***"
          sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
        }
        responseData = mSinglePropertyAvailabilityRequestHandler.execute( RoomAvailabilitySearchType.SINGLE_PROPERTY, requestData )
      }
      else if ( requestData.contains( '<OTA_HotelAvailRQ' ) && requestData.contains( 'PropertyList' ) )
      {
        if ( sLogger.isDebugEnabled() )
        {
          sLogger.debug "*** Rcvd an AREA AVAILABILITY REQUEST ***"
          sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
        }
        responseData = mAreaAvailabilityRequestHandler.execute( RoomAvailabilitySearchType.AREA_AVAILABILITY, requestData )
      }
      else if ( requestData.contains( '<amf>' ) )
      {
        requestData = requestData.substring( requestData.indexOf( '<amf>' ) + '<amf>'.length(), requestData.indexOf( '</amf>' ) )
        responseData = processAMFMessage( requestData )
      }
      else
      {
        StatisticsDashboard.unrecognizedRequestsReceivedCount.andIncrement
        sLogger.error "Unable to identify the following messsage received as a XML OTA or an AMF message:"
        sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
        sLogger.error "${requestData}"
        responseData = buildSoapFaultMessage( null, '99', 'Unable to identify the messsage received as a XML OTA or an AMF message' )
      }

      // Nothing to send for messages like Modify Reservation
      if ( responseData.equals('NO_RESPONSE_TO_SEND')) {
        resp.getWriter().flush()
      }
    }
    catch ( IllegalStateException es )
    {
      // On rare occasions, handlers and the processAMFMessage below can throw this Exception if they fail to parse or process the incoming request
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error es.message
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      responseData = buildSoapFaultMessage( null, '99', es.message )
    }
    catch ( Throwable e )
    {
      def errorMsg = "FATAL ERRROR - Couldn't process the message received from Pegasus USW, or caught a Request Processing Handler exception"
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error errorMsg
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error "Exception caught: ${e.message}", e
      responseData = buildSoapFaultMessage( null, '99', errorMsg )
    }

    resp.getWriter().println( responseData )
    resp.contentType = 'text/xml'
    resp.getWriter().flush()
    resp.getWriter().close()
    StatisticsDashboard.responsesSentCount.andIncrement
    StatisticsDashboard.lastServiceResponseTimestamp.set( new Date().dateTimeString )
  }

  /**
   * Process a Service Request that uses the Pegasus USW AMF specification.
   * <p />
   * @param requestData - Service Request data
   * @return String - Response to be sent to USW; null if received message type is unknown
   * @throws IllegalStateException - If the AMF message is missing a mandatory identification field
   */
  def private String processAMFMessage( String requestData )
  throws IllegalStateException
  {
    ReservationManagementType reservationMgtType = null

    if ( requestData.contains( 'BOOKRQ' ) )
    {
      if ( sLogger.isDebugEnabled() )
      {
        sLogger.debug "*** Rcvd an AMF RESERVATION MANAGEMENT REQUEST ***"
        sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      }

      if ( requestData.contains( 'BOOKRQ|ACTSS' ) || requestData.contains( 'BOOKRQ|ACTNN' ) ||
        requestData.contains( 'BOOKRQ|ACTIS' ) || requestData.contains( 'BOOKRQ|ACTIN' ) )
      {
        reservationMgtType = ReservationManagementType.NEW_RESERVATION
      }
      else if ( requestData.contains( 'BOOKRQ|ACTXX' ) || requestData.contains( 'BOOKRQ|ACTIX' ) )
      {
        reservationMgtType = ReservationManagementType.CANCEL_RESERVATION
      }
      else if ( requestData.contains( 'BOOKRQ|ACTHK' ) )
      {
        reservationMgtType = ReservationManagementType.MODIFY_RESERVATION
      }
      else if ( requestData.contains( 'BOOKRQ|ACTIG' ) )
      {
        reservationMgtType = ReservationManagementType.IGNORE_RESERVATION
      }
      else if ( requestData.contains( 'BOOKRQ|ACTET' ) )
      {
        reservationMgtType = ReservationManagementType.END_RESERVATION
      }

      return mAMFReservationManagementHandler.execute( reservationMgtType, requestData )
    }
    else if ( requestData.contains( 'RPINRQ' ) )
    {
      mAMFRateManagementHandler.execute( RateManagementType.RATE_PLAN_INFO, requestData )
    }
    else
    {
      sLogger.error "Unable to identify the following AMF request received:"
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error "${requestData}"
      throw new IllegalStateException( 'Unable to identify the received AMF message, missing a mandatory identification segment id')
    }
  }

  /**
   * Build a SOAP Fault message to be sent to Pegasus USW.
   * <p />
   * @param txnRef - The related transaction/Service Request
   * @param errorCode - Unique error code to set in the SOAP fault
   * @param errorMessage - Error message to set in the SOAP fault
   * @return String - SOAP Fault message built
   */
  def String buildSoapFaultMessage( txnRef, String errorCode, String errorMessage )
  {
    StringWriter writer = new StringWriter()

    try
    {
      def markupBuilder = new MarkupBuilder( writer )
      markupBuilder.omitEmptyAttributes = true
      markupBuilder.expandEmptyElements = false
      markupBuilder.omitNullAttributes = true

      // SOAP Envelope
      markupBuilder.'SOAP-ENV:Envelope'( 'xmlns:SOAP-ENV': 'http://schemas.xmlsoap.org/soap/envelope/' ) {
        // SOAP Header
        markupBuilder.'SOAP-ENV:Header'( 'xmlns:wsa': 'http://schemas.xmlsoap.org/ws/2004/08/addressing',
          'xmlns:wsse': 'http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd' )

        // SOAP Fault
        markupBuilder.'SOAP-ENV:Fault' {
          // OTA Response
          markupBuilder.'faultcode'( errorCode )
          markupBuilder.'faultstring'( errorMessage )
        } // SOAP Fault
      } // SOAP Envelope
    }
    catch ( Throwable e )
    {
      def errMsg = "Couldn't build a SOAP Fault message to be sent to Pegasus for Txn Ref: [${txnRef}] "
      sLogger.error errMsg, e
      return null
    }

    return writer.toString()
  }

}