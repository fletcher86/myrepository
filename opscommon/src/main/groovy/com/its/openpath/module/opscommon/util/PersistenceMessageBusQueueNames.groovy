package com.its.openpath.module.opscommon.util

/**
 * <code>MessageBusQueueNames.groovy</code>
 * <p/>
 * Class for defining message bus queue name constants used throughout the application
 * <p/>
 * @author kent
 * @since Jul 24, 2012
 */
class PersistenceMessageBusQueueNames
{
  /**
   * XML REQUEST QUEUE
   */
  public static final String REQUEST_XML_QUEUE="request.xml.queue"
  
  /**
   *  XML RESPONSE QUEUE
   */
  public static final String RESPONSE_XML_QUEUE="response.xml.queue"

  /**
   * JSON REQUEST QUEUE
   */
  public static final String REQUEST_JSON_QUEUE="request.json.queue"
  
  /**
   * JSON RESPONSE QUEUE
   */
  public static final String RESPONSE_JSON_QUEUE="response.json.queue"
  
  /**
   * READ JSON REQUEST QUEUE
   */
  public static final String READ_REQUEST_JSON_QUEUE="read.request.json.queue"
  
  /**
   * READ JSON RESPONSE QUEUE
   */
  public static final String READ_RESPONSE_XML_QUEUE="read.response.xml.queue"
  
  /**
   * READ JSON REQUEST QUEUE
   */
  public static final String READ_REQUEST_XML_QUEUE="read.request.xml.queue"
  
  /**
   * READ JSON RESPONSE QUEUE
   */

    public static final String READ_RESPONSE_JSON_QUEUE="read.response.json.queue"
  /**
   * TRANSACTION EVENT ERROR QUEUE
   */
  public static final String TXN_ERROR="transaction.event.error.queue"
  /**
   * TRANSACTION STATUS UPDATE QUEUE
   */
  public static final String TXN_STATUS_UPDATE="transaction.status.update.queue"
  
  /**
   * RESERVATIONS PULL REQUEST NOTIFICATION MESSAGE QUEUE
   */
  public static final String RESERVATIONS_NOTIFICATION_REPORT_JSON_REQUEST="reservations.report.json.request.notification"
  
  /**
   * RESERVATIONS PULL REQUEST NOTIFICATION MESSAGE QUEUE
   */
  public static final String RESERVATIONS_NOTIFICATION_REPORT_JSON_RESPONSE="reservations.report.json.response.notification"
  /**
   * RESERVATIONS PULL REQUEST NOTIFICATION MESSAGE QUEUE
   */
  public static final String RESERVATIONS_NOTIFICATION_REPORT_XML_SUCCESS_REQUEST="reservations.report.xml.success.request.notification"
  
  /**
   * RESERVATIONS PULL REQUEST NOTIFICATION MESSAGE QUEUE
   */
  public static final String RESERVATIONS_NOTIFICATION_REPORT_XML_SUCCESS_RESPONSE="reservations.report.xml.success.response.notification"
  /**
   * RESERVATIONS PULL REQUEST NOTIFICATION MESSAGE QUEUE
   */
  public static final String RESERVATIONS_NOTIFICATION_REPORT_XML_ERRORS_REQUEST="reservations.report.xml.errors.request.notification"
  
  /**
   * RESERVATIONS PULL REQUEST NOTIFICATION MESSAGE QUEUE
   */
  public static final String RESERVATIONS_NOTIFICATION_REPORT_XML_ERRORS_RESPONSE="reservations.report.xml.errors.response.notification"
}
