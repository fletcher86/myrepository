package com.its.openpath.module.siteminder.handlers


import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.util.StatusMonitoringEventMessageBusQueueNames
import com.its.openpath.module.opscommon.util.OpsStatuses
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

/**
 * <code>InProgressTxnHandler.groovy</code>
 * <p/>
 * If a transaction has been sitting around for a while 'IN_PROGRESS', mark it as failed
 * <p/>
 * @author Kent Fletcher
 * @since Sep 6, 2012
 */
@Service("InProgressTxnHandler")
@ManagedResource('OPENPATH:name=/module/siteminder/handlers/InProgressTxnHandler')
class InProgressTxnHandler extends AbstractSAFHandler
{
  private static final Logger sLogger = LoggerFactory.getLogger( InProgressTxnHandler.class.name )
  
  public static final String sStatus =  OpsStatuses.IN_PROGRESS
  
  public static final long expiration = 60000L
  
  InProgressTxnHandler()
  {
    super()
    sLogger.info "Instantiated ..."
  }
  
  @PostConstruct
  public void init()
  {
    super.init()
    mOpsMessageBus.consumeFromQueue( StatusMonitoringEventMessageBusQueueNames.IN_PROGRESS_MONITOR, this, false )
  }
  
  @Override
  public void onMessage( String topic, OpsMessage message )
  throws RuntimeException
  {
    sLogger.info "InProgressTxnHandler onMessage invoked ..."
    UUID uuid = TimeUUIDUtils.toUUID( message.getCorrelationIdBytes().toByteArray())
    
    long time = TimeUUIDUtils.getTimeFromUUID(uuid)
    
    long curtim = new Date().getTime()
    if ((curtim - time) > expiration)
    {
      /**
       * IF TRANSACTION HAS BEEN SITTING AROUND FOR A WHILE, THERE IS A PROBLEM, MARK IT AS FAILED 
       * AND LET ANOTHER HANDLER (RetryFailedTxnHandler) TAKE CARE IF IT
       */
      String xmlRequest = super.getPayloadAsString(uuid, "requestpayloadxml")
      if ( xmlRequest!=null && xmlRequest.trim().length()>0)
      {
        Date txnTimestatamp = new Date(curtim)
        sLogger.info "InProgressTxnHandler FOUND A TRANSACTION UUID=[${uuid}] TIMESTAMPED=[${txnTimestatamp}] 'IN_PROGRESS' THAT HASN'T MOVED FOR A WHILE, MARK IT AS FAILED AND LET ANOTHER HANDLER TAKE CARE OF IT ..."
        super.updateTransactionStatus ( uuid, OpsStatuses.FAILURE )
      }
    }
  }
}
