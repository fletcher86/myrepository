package com.its.openpath.module.siteminder.handlers

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.its.openpath.module.opscommon.model.messaging.ops.OpsErrorCode
import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.util.InvocationContext
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

@Service("RetryFailedTxnHandler")
@ManagedResource('OPENPATH:name=/module/siteminder/handlers/RetryFailedTxnHandler')
class RetryFailedTxnHandler extends AbstractSAFHandler
{
  private static final Logger sLogger = LoggerFactory.getLogger( RetryFailedTxnHandler.class.name )
  
  public static final String sStatus =  OpsStatuses.FAILURE
  
  RetryFailedTxnHandler()
  {
    super()
    sLogger.info "Instantiated ..."
  }
  
  @PostConstruct
  public void init()
  {
    super.init()
    mOpsMessageBus.consumeFromQueue( StatusMonitoringEventMessageBusQueueNames.TXN_RETRY, this, false )
  }
  
  @Override
  public void onMessage( String topic, OpsMessage message )
  throws RuntimeException
  {
    sLogger.info "RetryFailedTxnHandler onMessage invoked ..."
    UUID uuid = TimeUUIDUtils.toUUID( message.getCorrelationIdBytes().toByteArray())
    String xmlRequest = super.getPayloadAsString(uuid, "requestpayloadxml")
    super.postXmlSiteMinder(uuid, xmlRequest )  { int httpStatusCode, String baseUrl ->
      /**
       * INCREMENT THE NUMBER OF RETRIES
       */
      super.incrementIntColumn(uuid, "numretries")
      Integer currentRetryCount = super.getIntegerColumn(uuid, "numretries")
      /**
       * LOG A BIG FAT MESSAGE
       */
      sLogger.error(A_BUNCH_OF_XS)
      sLogger.error("ERROR POSTING TO SITEMINDER ENDPOINT UUID=[${uuid}]. NUMBER OF RETRIES=[${currentRetryCount}]. [${httpStatusCode}] BASE_URL=[${baseUrl}]")
      sLogger.error(A_BUNCH_OF_XS)
      return
    }
  }
}
