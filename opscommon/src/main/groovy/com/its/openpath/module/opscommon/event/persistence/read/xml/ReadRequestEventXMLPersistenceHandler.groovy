package com.its.openpath.module.opscommon.event.persistence.read.xml

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.its.openpath.module.opscommon.event.persistence.AbstractEventPersistenceHandler
import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.util.OpsStatuses
import com.its.openpath.module.opscommon.util.PersistenceMessageBusQueueNames
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

@Service("ReadRequestEventXMLPersistenceHandler")
@ManagedResource('OPENPATH:name=/module/opscommon/event/persistence/ReadRequestEventXMLPersistenceHandler')
class ReadRequestEventXMLPersistenceHandler extends AbstractEventPersistenceHandler
{
  private static Logger sLogger = LoggerFactory.getLogger(ReadRequestEventXMLPersistenceHandler.getName())
 
  /**
   * Constructor
   */
  ReadRequestEventXMLPersistenceHandler()
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
    super.mOpsMessageBus.consumeFromQueue( PersistenceMessageBusQueueNames.READ_REQUEST_XML_QUEUE, this, false )
  }
  
  /* (non-Javadoc)
   * @see com.its.openpath.module.opscommon.comm.bus.IMessageBusSubscriber#onMessage(java.lang.String, com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage)
   */
  @Override
  public void onMessage( String topic, OpsMessage message )
  throws RuntimeException
  {
    UUID uuid = null
    String source = null
    String destination = null
    Integer messageType = -1
    String messageSubType = null
    byte [] payloadBytes = null
    try
    {
      uuid = TimeUUIDUtils.toUUID ( message.getCorrelationIdBytes ().toByteArray () )
      source = message.getSource ()
      destination = message.getDestination ()
      String payload = message.getData ()
      if (payload!=null&& payload.trim ().length ()>0)
      {
        payloadBytes = payload.getBytes ()
      }
      else
      {
        payloadBytes = "".getBytes ()
      }
      
      this.persistPayload ( uuid, "readrequestxml", payloadBytes )
      this.updateTransactionStatus ( uuid, OpsStatuses.IN_PROGRESS )
    }
    catch (Throwable e)
    {
      this.persistError ( uuid, source, destination, messageType, messageSubType, e )
    }
  }
  
  
  @Override
  public String getSubscriberId()
  {
    return this.class.name;
  }

}
