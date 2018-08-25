package com.its.openpath.module.siteminder.builders

import groovy.xml.MarkupBuilder

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.dyuproject.protostuff.Message
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.HotelReservation
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.NotificationReportRequest
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

/**
 * <code>ReservationsConfirmedErrorRequestXMLBuilder</code>
 * <p/>
 * Concrete class for building ReservationsConfirmedErrorRequestXMLBuilder and inherits common functions from AbstractSiteminderXMLBuilder
 * <p />
 * @author Kent Fletcher
 * @since June 2012
 */
@Service("ReservationsConfirmedErrorRequestXMLBuilder")
@ManagedResource('OPENPATH:name=/module/siteminder/builder/ReservationsConfirmedErrorRequestXMLBuilder')
class ReservationsConfirmedErrorRequestXMLBuilder extends AbstractSiteminderXMLBuilder
{
  private static final Logger sLogger = LoggerFactory.getLogger( ReservationsConfirmedErrorRequestXMLBuilder.class.name )
  
  /**
   * Constructor
   */
  ReservationsConfirmedErrorRequestXMLBuilder( )
  {
    sLogger.info 'Instantiated ...'
  }
  
  /* (non-Javadoc)
   * @see com.its.openpath.module.siteminder.builders.AbstractSiteminderXMLBuilder#buildSoapXMLMessage(com.dyuproject.protostuff.Message)
   */
  @Override
  public String buildSoapXMLMessage( Message request )
  {
    sLogger.info ("INSIDE CLASS 'ReservationsConfirmedErrorRequestXMLBuilder' AND METHOD 'buildSoapXMLMessage' BUILDING SOAP XML MARKUP")
    NotificationReportRequest notifReport = (NotificationReportRequest) request
    UUID correlationId = TimeUUIDUtils.toUUID(notifReport.echoTokenBytes.getBytes ())
    
    Closure buildNotificationReport =
    { builder ->
      sLogger.info ("INSIDE CLOSURE 'ReservationsConfirmedErrorRequestXMLBuilder' AND METHOD 'buildSoapXMLMessage' BUILDING SOAP XML MARKUP")
      long time = TimeUUIDUtils.getTimeFromUUID(correlationId)
      builder.OTA_NotifReportRQ( xmlns: 'http://www.opentravel.org/OTA/2003/05', TimeStamp: new Date(time).toString(), Version: '1.0', EchoToken: correlationId )
      {
        Errors()
        {
          Error(Type: notifReport.reservationList[0].errorMessageType, Code: notifReport.reservationList[0].errorCode, notifReport.reservationList[0].errorMessage)
        }
        NotifDetails
        {
          HotelNotifReport
          {
            List<HotelReservation>hotelReservationList = notifReport.reservationList.findAll
            { (it.errorCode !=null) && (it.errorCode.trim().length()>0 ) }
            
            if(!hotelReservationList?.isEmpty())
            {
              HotelReservations
              {
                hotelReservationList.each
                { HotelReservation hotelReservation ->
                  HotelReservation(CreateDateTime: hotelReservation.createDateTime, ResStatus: hotelReservation.resStatus)
                  {
                    UniqueID(Type: hotelReservation.uniqueIdType, ID: hotelReservation.uniqueId)
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

