package com.its.openpath.module.siteminder.subscribers

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.its.openpath.module.opscommon.comm.bus.IMessageBus
import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.model.messaging.ops.OpsTxnType
import com.its.openpath.module.opscommon.util.EventMessageBusQueueNames
import com.its.openpath.module.siteminder.builders.ReservationsPullRequestXMLBuilder
import com.its.openpath.module.siteminder.builders.ReservationsPullResponseJSONBuilder



/**
 * <code>ReservationsPullNotifSubscriber</code>
 * <p/>
 * Consume Rate Update Notifications from the OPS Message Bus and POST them to the Siteminder IDS Web Service Endpoint.
 * The incoming Notification messages from the OPS bus are in the OPS JSON format, and the Notifications POSTed to
 * Siteminder are OTA XML encoded.
 * <p />
 * @author Lyle Fletcher
 * @since May 2012
 */

@Service("ReservationsPullNotifSubscriber")
@ManagedResource('OPENPATH:name=/module/siteminder/ReservationsPullNotifSubscriber')
class ReservationsPullNotifSubscriber extends AbstractSiteminderBaseSubscriber
{
  private static final Logger sLogger = LoggerFactory.getLogger( ReservationsPullNotifSubscriber.class.name )
  
  @Autowired(required = true)
  protected ReservationsPullRequestXMLBuilder mSoapXMLbuilder
  
  @Autowired(required = true)
  protected ReservationsPullResponseJSONBuilder mJsonBuilder
  
  /**
   * Constructor
   */
  ReservationsPullNotifSubscriber( )
  {
    sLogger.info 'Instantiated ...'
  }
  
  /**
   * Called by the Spring Framework after all Spring-manages beans are instantiated and wired up.
   */
  @PostConstruct
  void init( )
  {
    mOpsMessageBus.consumeFromQueue( EventMessageBusQueueNames.RESERVATIONS_PULL_REQUEST, this, true )
  }
  
  /* (non-Javadoc)
   * @see com.its.openpath.module.opscommon.comm.bus.IMessageBusSubscriber#getSubscriberId()
   */
  @Override
  public String getSubscriberId()
  {
    return this.class.name
  }
  
  /* (non-Javadoc)
   * @see com.its.openpath.module.opscommon.comm.bus.IMessageBusSubscriber#onMessage(java.lang.String, com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage)
   */
  @Override
  public void onMessage( String topic, OpsMessage opsMessage )
  throws RuntimeException
  {
    OpsTxnType type = OpsTxnType.RESERVATION_MGMT
    super.processMessage(opsMessage, type)
  }
}
