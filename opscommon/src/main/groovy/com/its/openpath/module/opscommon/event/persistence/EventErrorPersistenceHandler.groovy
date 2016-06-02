package com.its.openpath.module.opscommon.event.persistence

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.util.PersistenceMessageBusQueueNames
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

@Service("EventErrorPersistenceHandler")
@ManagedResource('OPENPATH:name=/module/opscommon/event/persistence/EventErrorPersistenceHandler')
public class EventErrorPersistenceHandler extends AbstractEventPersistenceHandler
{
  private static Logger sLogger = LoggerFactory.getLogger(EventErrorPersistenceHandler.getName())
  
  /**
   * Constructor
   */
  EventErrorPersistenceHandler()
  {
    sLogger.info "Instantiated ..."
  }
  
  /**
   * Called by the Spring Framework after all Spring-manages beans are instantiated and wired up.
   */
  @PostConstruct
  void init( )
  {
    super.init()
    super.mOpsMessageBus.consumeFromQueue( PersistenceMessageBusQueueNames.TXN_ERROR, this, false )
  }
  
  @Override
  public void onMessage( String topic, OpsMessage message )
  throws RuntimeException
  {
    try
    {
      UUID uuid = TimeUUIDUtils.toUUID ( message.getCorrelationIdBytes ().toByteArray () )
      String source = message.getSource ()
      String destination = message.getDestination ()
      String payload = message.getData ()
      Integer messageType = message.getMessageType ()
      String messageSubType = message.getMessageSubType ()
      byte [] payloadBytes = message.getData ()?message.getData ().getBytes() : "".getBytes ()
      this.persistError ( uuid, source, destination, messageType, messageSubType, payloadBytes )
    }
    catch(Throwable e)
    {
      sLogger.error("Could not persist transaction error for correlationId=[${uuid}] ", e)
    }
  }
  
  
  @Override
  public String getSubscriberId()
  {
    return this.class.name;
  }

}
