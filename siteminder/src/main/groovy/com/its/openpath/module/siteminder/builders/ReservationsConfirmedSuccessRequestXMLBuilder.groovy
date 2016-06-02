package com.its.openpath.module.siteminder.builders

import groovy.xml.MarkupBuilder

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.dyuproject.protostuff.Message
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.HotelReservation
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.NotificationReportRequest
import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

/**
 * <code>ReservationsConfirmedSuccessRequestXMLBuilder</code>
 * <p/>
 * Concrete class for building ReservationsConfirmedSuccessRequestXMLBuilder and inherits common functions from AbstractSiteminderXMLBuilder
 * <p />
 * @author Lyle Fletcher
 * @since June 2012
 */
@Service("ReservationsConfirmedSuccessRequestXMLBuilder")
@ManagedResource('OPENPATH:name=/module/siteminder/builder/ReservationsConfirmedSuccessRequestXMLBuilder')
class ReservationsConfirmedSuccessRequestXMLBuilder extends AbstractSiteminderXMLBuilder
{
  private static final Logger sLogger = LoggerFactory.getLogger( ReservationsConfirmedSuccessRequestXMLBuilder.class.name )
  
  /**
   * Constructor
   */
  ReservationsConfirmedSuccessRequestXMLBuilder()
  {
    sLogger.info 'Instantiated ...'
  }
  

  @Override
  public String buildSoapXMLMessage( Message request )
  {
    NotificationReportRequest notifReport = (NotificationReportRequest) request
    UUID correlationId = TimeUUIDUtils.toUUID(notifReport.echoTokenBytes.getBytes ())
    sLogger.info ("INSIDE CLASS 'ReservationsConfirmedSuccessRequestXMLBuilder' AND METHOD 'buildSoapXMLMessage' BUILDING SOAP XML MARKUP correlationId=[${correlationId}]")
    
    Closure buildNotificationReport =
    { builder ->
      UUID uuid = InvocationContext.instance.correlationId
      sLogger.info ("INSIDE CLOSURE 'ReservationsConfirmedSuccessRequestXMLBuilder' AND METHOD 'buildSoapXMLMessage' BUILDING SOAP XML MARKUP correlationId=[${correlationId}]")
      long time = TimeUUIDUtils.getTimeFromUUID(uuid)
      builder.OTA_NotifReportRQ( xmlns: 'http://www.opentravel.org/OTA/2003/05', TimeStamp: new Date(time).toString(), Version: '1.0', EchoToken: correlationId )
      {
        Success()
        NotifDetails
        {
          HotelNotifReport
          {
            List<HotelReservation>hotelReservationList = notifReport.reservationList.findAll
            { (it.resStatus !=null) && (it.resStatus.trim().equalsIgnoreCase("success") ) }
            
            if(!hotelReservationList?.isEmpty())
            {
              HotelReservations
              {
                hotelReservationList.each
                { HotelReservation hotelReservation ->
                  HotelReservation(CreateDateTime: hotelReservation.createDateTime, ResStatus: hotelReservation.resStatus)
                  {
                    UniqueID(Type: hotelReservation.uniqueIdType, ID: hotelReservation.uniqueId)
                    ResGlobalInfo
                    {
                      HotelReservationsIDs
                      {
                        HotelReservationID(ResID_Type: hotelReservation.resIdType, ResID_Value: hotelReservation.resIdValue)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    
    String msg = super.buildSOAPEnvelope(buildNotificationReport)
    
    if(sLogger.isDebugEnabled())
    {
      sLogger.info("MESSAGE = [${msg}]")
    }
    return msg
  }
}
