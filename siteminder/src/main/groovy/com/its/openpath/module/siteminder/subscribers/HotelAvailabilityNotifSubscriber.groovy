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
import com.its.openpath.module.siteminder.builders.HotelAvailabilityXMLBuilder

/**
 * <code>AbstractSiteminderBaseSubscriber</code>
 * <p/>
 * Base subscriber class
 * <p />
 * @author Lyle Fletcher
 * @since June 2012
 */

@Service("HotelAvailabilityNotifSubscriber")
@ManagedResource('OPENPATH:name=/module/siteminder/HotelAvailabilityNotifSubscriber')
class HotelAvailabilityNotifSubscriber extends AbstractSiteminderBaseSubscriber
{
  
  private static final Logger sLogger = LoggerFactory.getLogger( HotelAvailabilityNotifSubscriber.class.name )
  
  @Autowired(required = true)
  protected HotelAvailabilityXMLBuilder mSoapXMLbuilder
  
  /**
   * Constructor
   */
  HotelAvailabilityNotifSubscriber( )
  {
    sLogger.info 'Instantiated ...'
  }
  
  /**
   * Called by the Spring Framework after all Spring-manages beans are instantiated and wired up.
   */
  @PostConstruct
  void init( )
  {
    mOpsMessageBus.consumeFromQueue( EventMessageBusQueueNames.HOTEL_AVAIL_NOTIF, this, true )
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
    OpsTxnType type = OpsTxnType.AVAILABILITY_MGMT
    super.processMessage(opsMessage, type)
  }
 
}
