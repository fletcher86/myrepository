package com.its.openpath.module.siteminder.handlers


import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.util.OpsStatuses
import com.its.openpath.module.opscommon.util.StatusMonitoringEventMessageBusQueueNames
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

/**
 * <code>AbstractSAFHandler</code>
 * <p/>
 * Concrete class for building ReservationsPullResponseJSONBuilder
 * <p />
 * @author Kent Fletcher
 * @since June 2012
 */

@Service("ConfirmReservationsHandler")
@ManagedResource('OPENPATH:name=/module/siteminder/handlers/ConfirmReservationsHandler')
class ConfirmReservationsHandler extends AbstractSAFHandler
{
  private static final Logger sLogger = LoggerFactory.getLogger( ConfirmReservationsHandler.class.name )
  
  public static final String sStatus =  OpsStatuses.CONFIRM_RESERVATIONS
  
  ConfirmReservationsHandler()
  {
    super()
    sLogger.info "Instantiated ..."
  }
  
  @PostConstruct
  public void init()
  {
    super.init()
    mOpsMessageBus.consumeFromQueue( StatusMonitoringEventMessageBusQueueNames.RESERVATIONS_CONFIRMATION_NOTIF_REQUEST, this, false )
  }
  
  @Override
  public void onMessage( String topic, OpsMessage message )
  throws RuntimeException
  {
    sLogger.info "ConfirmReservationsHandler onMessage invoked ..."
    UUID uuid = TimeUUIDUtils.toUUID( message.getCorrelationIdBytes().toByteArray())
    String responseJson = super.getPayloadAsString(uuid, "readresponsejson")
    
    super.postJsonRezTripForConfirmation (uuid, responseJson) { 

      int httpStatusCode, String baseUrl ->
      /**
       * INCREMENT THE NUMBER OF RETRIES
       */
      super.incrementIntColumn(uuid, "numretries")
      
      Integer currentRetryCount = super.getIntegerColumn(uuid, "numretries")
      
      /**
       * LOG A BIG FAT MESSAGE
       */
      sLogger.error(A_BUNCH_OF_XS)
      sLogger.error("ERROR POSTING TO REZTRIP ENDPOINT UUID=[${uuid}]. NUMBER OF RETRIES=[${currentRetryCount}]. HTTP STATUS CODE=[${httpStatusCode}] BASE_URL=[${baseUrl}]")
      sLogger.error(A_BUNCH_OF_XS)
      return
    }
  }
}
