package com.its.openpath.module.siteminder.subscribers


import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.model.messaging.ops.OpsTxnType
import com.its.openpath.module.opscommon.util.EventMessageBusQueueNames
import com.its.openpath.module.siteminder.builders.ReservationsConfirmedSuccessRequestXMLBuilder

/**
 * <code>ReservationsSuccessNotifSubscriber</code>
 * <p/>
 * Consume successful reservation notifications from the OPS Message Bus and POST them to the Siteminder IDS Web Service Endpoint.
 * The incoming Notification messages from the OPS bus are in the OPS JSON format, and the Notifications POSTed to
 * Siteminder are OTA XML encoded.
 * <p />
 * @author Kent Fletcher
 * @since May 2012
 */

@Service("ConfirmedReservationsHandler")
@ManagedResource('OPENPATH:name=/module/siteminder/handlers/ConfirmedReservationsHandler')
class ReservationsSuccessNotifSubscriber extends AbstractSiteminderBaseSubscriber
{
  private static final Logger sLogger = LoggerFactory.getLogger( ReservationsSuccessNotifSubscriber.class.name )
  
  @Autowired(required = true)
  protected ReservationsConfirmedSuccessRequestXMLBuilder mSoapXMLbuilder

  ReservationsSuccessNotifSubscriber()
  {
    super()
    sLogger.info "Instantiated ..."
  }
  
  @PostConstruct
  public void init()
  {
    mOpsMessageBus.consumeFromQueue( EventMessageBusQueueNames.RESERVATIONS_CONFIRMATION_SUCCESS_NOTIF_REQUEST, this, false )
  }
  
  @Override
  public String getSubscriberId()
  {
    return this.class.name
  }
  
  @Override
  public void onMessage( String topic, OpsMessage opsMessage )
  throws RuntimeException
  {
    OpsTxnType type = OpsTxnType.RESERVATION_NOTIFICATION_REPORT_SUCCESS_MGMT
    super.processMessage(opsMessage, type)
  }
}
