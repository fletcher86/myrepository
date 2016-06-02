package com.its.openpath.module.opscommon.event.persistence

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.util.PersistenceMessageBusQueueNames
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

@Service("UpdateTxnStatusPersistenceHandler")
@ManagedResource('OPENPATH:name=/module/opscommon/event/persistence/UpdateTxnStatusPersistenceHandler')
public class UpdateTxnStatusPersistenceHandler extends AbstractEventPersistenceHandler
{
  private static Logger sLogger = LoggerFactory.getLogger(UpdateTxnStatusPersistenceHandler.getName())
  
  /**
   * Constructor
   */
  UpdateTxnStatusPersistenceHandler()
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
    super.mOpsMessageBus.consumeFromQueue( PersistenceMessageBusQueueNames.TXN_STATUS_UPDATE, this, false )
  }
  
  @Override
  public void onMessage( String topic, OpsMessage message )
  throws RuntimeException
  {
    try
    {
      UUID uuid = TimeUUIDUtils.toUUID ( message.getCorrelationIdBytes ().toByteArray () )
      String status = message.data
      this.updateTransactionStatus(uuid, status);
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
