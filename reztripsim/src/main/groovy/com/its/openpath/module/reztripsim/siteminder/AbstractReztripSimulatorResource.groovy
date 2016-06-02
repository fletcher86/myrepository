package com.its.openpath.module.reztripsim.siteminder

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

import javax.ws.rs.core.Response

import org.apache.cxf.helpers.IOUtils
import org.apache.cxf.jaxrs.client.WebClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.dyuproject.protostuff.Message

class AbstractReztripSimulatorResource
{
  private static final Logger sLogger = LoggerFactory.getLogger( AbstractReztripSimulatorResource.class.name )

  /**
   * Helper method that POST the supplied JSON encoded Service Request to the OPSARI REST Endpoint.
   * <p />
   * @param requestJSON - JSON Service Request message in OpenPath format
   */
  def postRequest( String requestJSON, String endpoint, Closure handleResponse )
  {
    
    WebClient wclient = WebClient.create( endpoint )
    
    Response response
    
    /*
     * Pick a random number between 1 and 100
     */
    Random rand = new Random()
    int randomPick = rand.nextInt( 100 )
    
    def txnRef = 'A'+randomPick
    
    try
    {
      wclient.type( "application/json" ).accept( "application/json" )
      if ( sLogger.isDebugEnabled() )
      {
        sLogger.debug "*** REZTRIPSIM -  POSTing Service Request: [${txnRef}] to OPS REST Endpoint: [${wclient.currentURI}]"
      }
      response = wclient.post( requestJSON )
      
      if ( response.status != 200 )
      {
        logAndHandlePOSTerror( "*** REZTRIPSIM - couldn't POST to to OPS REST Endpoint: [${wclient.currentURI}], HTTP Error Code Rcvd: ${response.status}", null )
      }
      else
      {
        if ( sLogger.isDebugEnabled() )
        {
          sLogger.debug "*** REZTRIPSIM - POSTed Service Request: [${txnRef}] to OPS REST Endpoint: [${wclient.currentURI}]"
        }
        InputStream inputStream = (InputStream) response.getEntity()
        String responseJson = IOUtils.toString( inputStream )
        sLogger.info("Recieved ack =[${responseJson}]")
        handleResponse( responseJson )
      }
    }
    catch ( Throwable e )
    {
      logAndHandlePOSTerror(  "Couldn't POST to: [${wclient.currentURI.toString()}]", e )
    }
  }
  
  /**
   * Helper method to handle and error that occurred during POSTing to OPSARI Endpoint.
   * <p />
   * @param errorMessage - Descriptive error message
   * @param e - Exception caught
   */
  def logAndHandlePOSTerror( String errorMessage, Throwable e )
  {
    sLogger.error "**** REZTRIPSIM - *************************************************************************************************"
    sLogger.error "*** REZTRIPSIM - ${errorMessage}", e
    sLogger.error "*** REZTRIPSIM - *************************************************************************************************"
  }
}
