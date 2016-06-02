package com.its.openpath.module.opscommon.util

/**
 * <code>MessageBusQueueNames.groovy</code>
 * <p/>
 * Class for defining message bus queue name constants used throughout the application
 * <p/>
 * @author kent
 * @since Jul 24, 2012
 */
class EventMessageBusQueueNames
{
  /**
   * RATE NOTIFICATION QUEUE
   */
  public static final String RATE_MGMT_NOTIF="rate.management.notification"
  
  /**
   * HOTEL AVAILABILITY NOTIFICATION QUEUE
   */
  public static final String HOTEL_AVAIL_NOTIF="hotel.availablity.notification"
  
  /**
   * RESERVATIONS PULL REQUEST NOTIFICATION MESSAGE QUEUE
   */
  public static final String RESERVATIONS_PULL_REQUEST="reservations.pull.request.notification"
  
  /**
   * RESERVATIONS CONFIRMATION REQUEST NOTIFICATION MESSAGE QUEUE
   */
  public static final String RESERVATIONS_CONFIRMATION_ERROR_NOTIF_REQUEST="reservations.confirmation.error.request.notification"

  /**
   * RESERVATIONS CONFIRMATION REQUEST NOTIFICATION MESSAGE QUEUE
   */
  public static final String RESERVATIONS_CONFIRMATION_SUCCESS_NOTIF_REQUEST="reservations.confirmation.success.request.notification"
}
