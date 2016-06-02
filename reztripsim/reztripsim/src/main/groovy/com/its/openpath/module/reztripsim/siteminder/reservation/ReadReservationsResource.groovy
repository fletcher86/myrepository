package com.its.openpath.module.reztripsim.siteminder.reservation
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.Response

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jmx.export.annotation.ManagedOperation
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.dyuproject.protostuff.JsonIOUtil
import com.its.openpath.module.opscommon.model.messaging.ops.ProductType
import com.its.openpath.module.opscommon.model.messaging.ops.Source
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.ReservationManagementType
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.ReservationPullRequest
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.ReservationRequest
import com.its.openpath.module.reztripsim.siteminder.AbstractReztripSimulatorResource


@Service("ReadReservationsResource")
@ManagedResource('OPENPATH:name=/module/reztripsim/siteminder/reservation/ReadReservationsResource')
@Path("/read")
class ReadReservationsResource extends AbstractReztripSimulatorResource
{
  private static final Logger sLogger = LoggerFactory.getLogger( ReadReservationsResource.class.name )
  
  private @Value("#{runtimeProperties['opsari.base.url']}")
  String mOpsAriBaseURL
  
  private @Value("#{runtimeProperties['opsari.readReservations.uri']}")
  String mOpsAriReadReserverationsURI
  
  
  /**
   * Constructor
   */
  ReadReservationsResource ()
  {
    sLogger.info 'Instantiated ...'
  }
  
  
  /**
   * Generate a Read Reservation Request with test data and POST it to OPSARI module's REST Endpoint.
   * <p />
   * http://[HOST_NAME]/reztripsim/rs/rate/update
   */
  @POST
  @Path('/reservations')
  @ManagedOperation()
  def Response generateReadReservationsRequest( )
  {
    sLogger.info 'Generating a read reservation request ...'
    String requestJSON
    String endpoint = "${mOpsAriBaseURL}${mOpsAriReadReserverationsURI}"
    try
    {
      ReservationRequest resRequest = new ReservationRequest()
      resRequest.productType = ProductType.HOTEL_ROOM
      resRequest.requestData = new ReservationPullRequest()
      resRequest.requestData.reservationMgtType = ReservationManagementType.PULL_RESERVATIONS
      resRequest.requestData.extSysRefId = '1234'
      resRequest.requestData.extSysTimestamp = new Date().time
      
      resRequest.requestData.source = new Source()
      resRequest.requestData.source.id = 'ABC'
      resRequest.requestData.source.type = 'HOTEL'
      resRequest.requestData.source.description = 'RESERVATION PULL REQUEST'
      
      Writer writer = new StringWriter()
      JsonIOUtil.writeTo( writer, resRequest, resRequest.cachedSchema(), false )
      requestJSON = writer.toString()
      sLogger.info "*** REZTRIPSIM - built a IDSReservationPullRequest JSON message to be POSTed to OPSARI module ..."
    }
    catch ( Throwable e )
    {
      this.logAndHandlePOSTerror( "*** REZTRIPSIM - Couldn't create a IDSReservationPullRequest to be POSTed to OPS", e )
      return
    }
    
    Response responseObj = Response.serverError().build()
    
    // POST to OPSARI
    postRequest( requestJSON, endpoint ) { String responseJSON ->
      // Not doing anything with the response yet
      sLogger.info "*** REZTRIPSIM - Received the response below from OPSARI REST Endpoint as a response for Rate Update Notification"
      sLogger.info "*************************************************************************************************"
      sLogger.info "${responseJSON}"
      responseObj = Response.ok( responseJSON ).status( 200 ).build()
    }
    
    return responseObj
  }
  
}
