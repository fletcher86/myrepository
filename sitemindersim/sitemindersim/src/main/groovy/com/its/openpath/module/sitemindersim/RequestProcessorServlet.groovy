package com.its.openpath.module.sitemindersim

import com.its.openpath.module.sitemindersim.handler.RateUpdateRequestHandler
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
 * The Servlet that process all incoming OTA XML Service Requests and simulate message 'PUSH' to Siteminder. Delegates
 * service request processing to one of the 'Handler' instances.
 * <p />
 * @author rajiv@itstcb.com
 * @since Aug 2012
 */
class RequestProcessorServlet
extends HttpServlet
{
  Logger sLogger = LoggerFactory.getLogger( RequestProcessorServlet.class.name )

  private ServletContext mServletContext
  private RateUpdateRequestHandler mRateUpdateRequestHandler = new RateUpdateRequestHandler()



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

    //mSinglePropertyAvailabilityRequestHandler = ctx.getBean( XMLSinglePropertyAvailabilityRequestHandler.class );
  }

  /**
   * @see {@link HttpServlet#doPost}
   */
  @Override
  protected void doPost( final HttpServletRequest req, final HttpServletResponse resp )
  {
    String requestData

    try
    {
      requestData = req.getReader().text

      // Need to change this, add more qualifiers to determine the type of message received
      if ( requestData.contains( 'ratePlans' ) )
      {
        if ( sLogger.isDebugEnabled() )
        {
          sLogger.debug "*** SITEMINDERSIM - Rcvd a Rate Update REQUEST ***"
          sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
        }
        mRateUpdateRequestHandler.process( requestData )
      }
      else
      {
        sLogger.error "SITEMINDERSIM - Received an UNKNOWN request, unable to process the following messsage"
        sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
        sLogger.error "${requestData}"
        resp.setStatus( 500 )
      }
    }
    catch ( Throwable e )
    {
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error "SITEMINDERSIM - FATAL ERRROR - Couldn't process the message received, or caught a Request Processing Handler exception"
      sLogger.error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.error "Exception caught: ${e.message}", e
      resp.setStatus( 500 )
      //@TODO - Is sending 500 good enough
    }
    finally
    {
      resp.getWriter().close()
    }
  }

}