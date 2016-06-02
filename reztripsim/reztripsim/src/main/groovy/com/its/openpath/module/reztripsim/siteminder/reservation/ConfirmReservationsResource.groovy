package com.its.openpath.module.reztripsim.siteminder.reservation

import org.slf4j.Logger

import com.its.openpath.module.reztripsim.siteminder.AbstractReztripSimulatorResource

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.dyuproject.protostuff.JsonIOUtil
import com.dyuproject.protostuff.Message
import com.its.openpath.module.opscommon.model.messaging.ops.Ack
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.HotelReservation
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.NotificationReportRequest
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.ReservationResponse
import com.its.openpath.module.opscommon.util.OpsStatuses
import com.its.openpath.module.reztripsim.siteminder.AbstractReztripSimulatorResource
import com.its.openpath.module.reztripsim.siteminder.ResConfirmationMonitorThread
@Service("ConfirmReservationsResource")
@ManagedResource('OPENPATH:name=/module/reztripsim/siteminder/reservation/ConfirmReservationsResource')
@Path("/confirm")
class ConfirmReservationsResource extends AbstractReztripSimulatorResource
{
  private static final Logger sLogger = LoggerFactory.getLogger( ConfirmReservationsResource.class.name )
  // The URL or POST header values doesn't change per-request, so using an instance like this is thread safe
  
  public static Map<String, Message> confirmationMap = new HashMap<String, Message>()
  
  public static Map<String, Message> confirmedMap = new HashMap<String, Message>()
  
  public final ScheduledExecutorService mScheduler = Executors.newSingleThreadScheduledExecutor()
  
  
  private @Value("#{runtimeProperties['opsari.base.url']}")
  String mOpsAriBaseURL
  
  private @Value("#{runtimeProperties['opsari.reservation.notification.report.uri']}")
  String mOpsAriConfirmReserverationsURI
  
  /**
   * Constructor
   */
  ConfirmReservationsResource ()
  {
    sLogger.info 'Instantiated ...'
  }
  
  /**
   * This init launches two thread to monitor reservations in a 'confirmationMap' (pre confirm) and a 'confirmedMap' (post confirm)
   *<p />
   */
  @PostConstruct
  public void init()
  {
    /**
     * This closure is used to monitor internal cache of reservations to be confirmed.
     */
    Closure confirmCodeFrag =
    {
      if(!confirmationMap.isEmpty())
      {
        Set<String> keys = confirmationMap.keySet()
        for(String uuid : keys)
        {
          sLogger.info("CONFIRMATION MAP HAS DATA UUID = [${uuid}]")
          
          Message msg = confirmationMap.get(uuid )
          
          ReservationResponse reservations = (ReservationResponse) msg
          
          for (HotelReservation res : reservations.responseData?.reservationList )
          {
            /*
             * ITERATE THROUGH HOTEL RESERVATIONS AND FLIP A COIN TO DESIGNATE IF THE RESERVATION WAS A SUCCESS OR FAILURE
             */
            Random rand = new Random()
            int randomPick = rand.nextInt( 100 )
            /*
             * Return success message if < 50
             */
            if(randomPick <= 50)
            {
              res.resStatus = "SUCCESS"
            }
            else
            {
              res.resStatus = "ERROR"
              res.errorMessageType = "3"
              res.errorCode = "402"
              res.errorMessage = "Invalid Room Type =[${randomPick}]"
            }
          }
          /**
           * REMOVE RESERVATION FROM CONFIRMATION MAP AND PUT IN CONFIRMED MAP
           */
          confirmationMap.remove(uuid)
          confirmedMap.put ( uuid, msg )
        }
      }
    }
    
    /**
     * THIS CLOSURE IS USED TO ITERATE THROUGH THE CONFIRMED MAP AND POST BACK TO OPSARI MODULE A NOTIFICATION REPORT
     */
    Closure confirmedFrag = {
      if(!confirmedMap.isEmpty())
      {
        Set<String> keys = confirmedMap.keySet()
        for(String uuid : keys)
        {
          sLogger.info("CONFIRMED MAP HAS DATA UUID = [${uuid}]")
          
          Message msg = confirmedMap.get(uuid )
          ReservationResponse response = (ReservationResponse) msg
          postNotificationReport(response)
          confirmedMap.remove(uuid)
        }
      }
    }
    
    def confirmAction = new ResConfirmationMonitorThread(confirmCodeFrag)
    def confirmedAction = new ResConfirmationMonitorThread(confirmedFrag)
    mScheduler.scheduleWithFixedDelay( confirmAction, 20, 20, TimeUnit.SECONDS )
    mScheduler.scheduleWithFixedDelay( confirmedAction, 10, 20, TimeUnit.SECONDS )
  }
  
  /**
   * POST NOTIFICATION REPORT TO OPSARI MODULE
   *<p />
   *@param response ReservationResponse
   *@return Response
   */
  def Response postNotificationReport(ReservationResponse response)
  {
    NotificationReportRequest reportRequest = new NotificationReportRequest()
    reportRequest.echoToken = response.responseData.echoToken
    reportRequest.echoTokenBytes = response.responseData.echoTokenBytes
    reportRequest.status = response.responseData.isSuccess
    reportRequest.reservationList = response.responseData.reservationList
    
    sLogger.info 'Posting Notification Report request ... for [${reportRequest.echoToken}]'
    String requestJSON
    String endpoint = "${mOpsAriBaseURL}${mOpsAriConfirmReserverationsURI}"
    try
    {
      Writer writer = new StringWriter()
      JsonIOUtil.writeTo( writer, reportRequest, reportRequest.cachedSchema(), false )
      requestJSON = writer.toString()
      sLogger.info "*** REZTRIPSIM - Sending a NotificationReportRequest JSON message to be POSTed to OPSARI module ..."
    }
    catch ( Throwable e )
    {
      this.logAndHandlePOSTerror( "*** REZTRIPSIM - Couldn't create a  to be POSTed to OPS", e )
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
  
  @POST
  @Path('/reservations')
  @Consumes("application/json")
  @Produces("application/json")
  def Response confirmReservationsRequest(@Context HttpServletRequest servletRequest )
  {
    sLogger.info 'Confirm Reservation ...'
    String responseJSON = servletRequest.inputStream.text
    
    ReservationResponse res = new ReservationResponse()
    
    try
    {
      JsonIOUtil.mergeFrom( responseJSON.bytes, res, ReservationResponse.schema, false )
    }
    catch (Throwable e)
    {
      sLogger.error("Error marshalling Json ReservationResponse = [${responseJSON}]")
    }
    
    String correlationId = res.responseData?.echoToken
    
    for(HotelReservation r :res.responseData?.reservationList)
    {
      sLogger.info("**reservation received for confirmation** timestamp=[${r.createDateTime}] restatus=[${r.resStatus}] uniqueIdType=[${r.uniqueIdType}] uniqueId=[${r.uniqueId}] resIdType=[${r.resIdType}] resIdValue=[${r.resIdValue}]")
    }
    
    confirmationMap.put ( correlationId, res )
    
    sLogger.info 'Confirm Reservation ...${responseJSON}'
    Ack ack = new Ack()
    ack.id = correlationId
    ack.status = OpsStatuses.SUCCESS
    
    
    StringWriter writer = new StringWriter()
    JsonIOUtil.writeTo( writer, ack, ack.cachedSchema(), false )
    
    String responseAck =  writer.toString()
    javax.ws.rs.core.Response responseObj = Response.ok( responseAck ).status( 200 ).build()
    return responseObj
  }
}
