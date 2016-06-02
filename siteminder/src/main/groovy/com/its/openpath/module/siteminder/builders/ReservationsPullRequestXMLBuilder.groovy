package com.its.openpath.module.siteminder.builders

import groovy.xml.MarkupBuilder

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.dyuproject.protostuff.Message
import com.its.openpath.module.opscommon.model.messaging.ops.OpsTxnType
import com.its.openpath.module.opscommon.model.messaging.ops.reservation.siteminder.ReservationRequest
import com.its.openpath.module.opscommon.util.InvocationContext
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

/**
 * <code>ReservationsPullRequestXMLBuilder</code>
 * <p/>
 * Concrete class for building ReservationsPullRequestXMLBuilder and inherits common functions from AbstractSiteminderXMLBuilder
 * <p />
 * @author Lyle Fletcher
 * @since June 2012
 */
@Service("ReservationsPullRequestXMLBuilder")
@ManagedResource('OPENPATH:name=/module/siteminder/builder/ReservationsPullRequestXMLBuilder')
class ReservationsPullRequestXMLBuilder extends AbstractSiteminderXMLBuilder
{
  private static final Logger sLogger = LoggerFactory.getLogger( ReservationsPullRequestXMLBuilder.class.name )
  
  /**
   * Constructor
   */
  ReservationsPullRequestXMLBuilder( )
  {
    sLogger.info 'Instantiated ...'
  }
  
  @Override
  public String buildSoapXMLMessage( Message request )
  {
    ReservationRequest reservationRequest = (ReservationRequest) request
    return super.buildSOAPEnvelope { builder ->
      UUID uuid = InvocationContext.instance.correlationId
      long time = TimeUUIDUtils.getTimeFromUUID(uuid)
      
      builder.OTA_ReadRQ( xmlns: 'http://www.opentravel.org/OTA/2003/05', TimeStamp: new Date(time).toString(), Version: '1.0', EchoToken: uuid )
      {
        ReadRequests(HotelCode: reservationRequest.requestData.source.id)
        {
          HotelReadRequest(HotelCode: reservationRequest.requestData.source.id)
          {
            SelectionCriteria(SelectionType: reservationRequest.requestData.selectionCriteria)
          }
        }
      }
    }
  }
}
