package com.its.openpath.module.sitemindersim.handler

import com.its.openpath.module.opscommon.util.InvocationContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

/**
 * <code>RateUpdateRequestHandler</code>
 * <p/>
 * Handles Rate Update Service Requests POSTED from the siteminder module simulating the behaviour of message 'PUSH' to a
 * Siteminder WS Endpoint.
 * <p />
 * <p />
 * @author rajiv@itstcb.com
 * @since Aug 2012
 */

@Service
@ManagedResource('OPENPATH:name=/module/sitemindersim/handler/RateUpdateRequestHandler')
class RateUpdateRequestHandler
{
  private static final Logger sLogger = LoggerFactory.getLogger( RateUpdateRequestHandler.class.name )


  /**
   * Constructor
   */
  RateUpdateRequestHandler( )
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
      sLogger.debug "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
      sLogger.debug "[${requestXML}]"
    }

    return responseXML
  }

}
