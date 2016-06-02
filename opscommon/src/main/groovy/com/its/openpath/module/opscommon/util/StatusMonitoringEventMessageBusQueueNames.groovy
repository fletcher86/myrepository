package com.its.openpath.module.opscommon.util

/**
 * <code>MessageBusQueueNames.groovy</code>
 * <p/>
 * Class for defining message bus queue name constants used throughout the application
 * <p/>
 * @author kent
 * @since Jul 24, 2012
 */
class StatusMonitoringEventMessageBusQueueNames
{

  
  /**
   * TRANSACTION RETRY EVENT QUEUE
   */
  public static final String TXN_RETRY="transaction.event.retry.queue"

  /**
   * QUEUE FOR MONITORING 'IN_PROGRESS' TRANSACTIONS
   */
  public static final String IN_PROGRESS_MONITOR="transaction.event.in.progress.queue"
  
  /**
   * RESERVATIONS CONFIRMATION REQUEST NOTIFICATION MESSAGE QUEUE
   */
  public static final String RESERVATIONS_CONFIRMATION_NOTIF_REQUEST="reservations.confirmation.request.notification"

  /**
   * RESERVATIONS CONFIRMATION REQUEST NOTIFICATION MESSAGE QUEUE
   */
  public static final String RESERVATIONS_CONFIRMED_NOTIF_REQUEST="reservations.confirmed.request.notification"

}
