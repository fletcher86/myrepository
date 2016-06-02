package com.its.openpath.module.opscommon.event.persistence

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.util.OpsStatuses
import com.its.openpath.module.opscommon.util.PersistenceMessageBusQueueNames
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

@Service("RequestEventPersistenceHandler")
@ManagedResource('OPENPATH:name=/module/opscommon/event/persistence/RequestEventPersistenceHandler')
class RequestEventXMLPersistenceHandler extends AbstractEventPersistenceHandler
{
  private static Logger sLogger = LoggerFactory.getLogger(RequestEventXMLPersistenceHandler.getName())
 
  /**
   * Constructor
   */
  RequestEventXMLPersistenceHandler()
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
    super.mOpsMessageBus.consumeFromQueue( PersistenceMessageBusQueueNames.REQUEST_XML_QUEUE, this, false )
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
      
      if(source!=null&& source.toLowerCase ().trim ().indexOf ( "pegasus" )>=0)
      {
        messageType = message.getMessageType ()?:-1
        messageSubType = message.getMessageSubType ()
        this.persistRequest ( uuid, source, destination, messageType, messageSubType, "requestpayloadxml", payloadBytes )
      }
      else
      {
        this.persistPayload ( uuid, "requestpayloadxml", payloadBytes )
      }
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
