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
import com.its.openpath.module.opscommon.model.messaging.ops.OpsTxnType
import com.its.openpath.module.opscommon.util.EventMessageBusQueueNames
import com.its.openpath.module.siteminder.builders.RateManagementXMLBuilder

/**
 * <code>RateManagementNotifSubscriber</code>
 * <p/>
 * Consume Rate Update Notifications from the OPS Message Bus and POST them to the Siteminder IDS Web Service Endpoint.
 * The incoming Notification messages from the OPS bus are in the OPS JSON format, and the Notifications POSTed to
 * Siteminder are OTA XML encoded.
 * <p />
 * @author Kent Fletcher
 * @since May 2012
 */

@Service("RateManagementNotifSubscriber")
@ManagedResource('OPENPATH:name=/module/siteminder/RateManagementNotifSubscriber')
class RateManagementNotifSubscriber extends AbstractSiteminderBaseSubscriber
{
  private static final Logger sLogger = LoggerFactory.getLogger( RateManagementNotifSubscriber.class.name )
  
  @Autowired(required = true)
  protected RateManagementXMLBuilder mSoapXMLbuilder
  
  /**
   * Constructor
   */
  RateManagementNotifSubscriber( )
  {
    sLogger.info 'Instantiated ...'
  }
  
  
  /**
   * Called by the Spring Framework after all Spring-manages beans are instantiated and wired up.
   */
  @PostConstruct
  void init( )
  {
    mOpsMessageBus.consumeFromQueue( EventMessageBusQueueNames.RATE_MGMT_NOTIF, this, true )
  }
  
  /**
   * @see {@link com.its.openpath.module.opscommon.comm.bus.IMessageBusSubscriber#getSubscriberId}
   */
  String getSubscriberId( )
  {
    return this.class.name
  }
  
  
  @Override
  public void onMessage( String topic, OpsMessage opsMessage )
  throws RuntimeException
  {
    OpsTxnType type = OpsTxnType.RATE_MGMT
    super.processMessage(opsMessage, type)
  }
 
}
