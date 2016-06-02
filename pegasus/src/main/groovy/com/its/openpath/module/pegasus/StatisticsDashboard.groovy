package com.its.openpath.module.pegasus

import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedOperation
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * <code>StatisticsDashboard</code>
 * <p/>
 * Display various runtime statistics as JMX attributes
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service
@ManagedResource('OPENPATH:name=/module/pegasus/StatisticsDashboard')
class StatisticsDashboard
{
  // Total of all Service Request Messages received
  static AtomicLong requestsReceivedCount = new AtomicLong( 0 )

  // Total of all Service Response Messages sent
  static AtomicLong responsesSentCount = new AtomicLong( 0 )

  // Total of all Errors where a Error Response was sent
  static AtomicLong errorsWithResponseSentCount = new AtomicLong( 0 )

  // Total of all unrecognized/unparsable Service Requests received
  static AtomicLong unrecognizedRequestsReceivedCount = new AtomicLong( 0 )

  // Timestamp of last incoming Service Request
  static AtomicReference lastServiceRequestTimestamp = new AtomicReference()

  // Timestamp of last incoming Service Response sent
  static AtomicReference lastServiceResponseTimestamp = new AtomicReference()

  // Short description of last incoming Service Request
  static AtomicReference lastServiceRequestInfo = new AtomicReference()



  // Total of Area Availability Requests received
  static AtomicLong areaAvailabilityRequestCount = new AtomicLong( 0 )

  // Total of Single Property Availability Requests received
  static AtomicLong singlePropertyAvailabilityRequestCount = new AtomicLong( 0 )

  // Total of Rate Plan Info Requests received
  static AtomicLong ratePlanInfoRequestCount = new AtomicLong( 0 )

  // Total of New Reservation/Booking Requests received
  static AtomicLong newReservationRequestCount = new AtomicLong( 0 )

  // Total of Modify Reservation/Booking Requests received
  static AtomicLong modifyReservationRequestCount = new AtomicLong( 0 )

  // Total of Cancel Reservation/Booking Requests received
  static AtomicLong cancelReservationRequestCount = new AtomicLong( 0 )

  // Total of Ignore Reservation/Booking Requests received
  static AtomicLong ignoreReservationRequestCount = new AtomicLong( 0 )

  // Total of End Reservation/Transation Requests received
  static AtomicLong endTransactionRequestCount = new AtomicLong( 0 )





  @ManagedAttribute(description = 'Total of all Service Request Messages received')
  long getRequestsReceived( )
  {
    return requestsReceivedCount.get()
  }

  @ManagedAttribute(description = 'Total of all Service Response Messages sent')
  long getResponsesSent( )
  {
    return responsesSentCount.get()
  }

  @ManagedAttribute(description = 'Total of all Errors where a Error Response was sent')
  long getErrorsWithResponseSent( )
  {
    return errorsWithResponseSentCount.get()
  }

  @ManagedAttribute(description = 'Total of all unrecognized/unparsable Service Requests received')
  long getUnrecognizedRequestsReceived( )
  {
    return unrecognizedRequestsReceivedCount.get()
  }

  @ManagedAttribute(description = 'Timestamp of last incoming Service Request')
  String getLastServiceRequestTimestamp( )
  {
    return lastServiceRequestTimestamp.get()
  }

  @ManagedAttribute(description = 'Timestamp of last incoming Service Response sent')
  String getLastServiceResponseTimestamp( )
  {
    return lastServiceResponseTimestamp.get()
  }

  @ManagedAttribute(description = 'Type of last incoming Service Request')
  String getLastServiceRequestType( )
  {
    return lastServiceRequestInfo.get()
  }

  @ManagedAttribute(description = 'Total of Area Availability Requests received')
  long getAreaAvailabilityRequests( )
  {
    return areaAvailabilityRequestCount.get()
  }

  @ManagedAttribute(description = 'Total of Single Property Availability Requests received')
  long getSinglePropertyAvailabilityRequests( )
  {
    return singlePropertyAvailabilityRequestCount.get()
  }

  @ManagedAttribute(description = 'Total of Rate Plan Info Requests received')
  long getRatePlanInfoRequests( )
  {
    return ratePlanInfoRequestCount.get()
  }

  @ManagedAttribute(description = 'Total of New Reservation/Booking Requests received')
  long getNewReservationRequests( )
  {
    return newReservationRequestCount.get()
  }

  @ManagedAttribute(description = 'Total of Modify Reservation/Booking Requests received')
  long getModifyReservationRequests( )
  {
    return modifyReservationRequestCount.get()
  }

  @ManagedAttribute(description = 'Total of Cancel Reservation/Booking Requests received')
  long getCancelReservationRequests( )
  {
    return cancelReservationRequestCount.get()
  }

  @ManagedAttribute(description = 'Total of Ignore Reservation/Booking Requests received')
  long getIgnoreReservationRequests( )
  {
    return ignoreReservationRequestCount.get()
  }

  @ManagedAttribute(description = 'Total of End Reservation/Transation Requests received')
  long getEndTransactionRequests( )
  {
    return endTransactionRequestCount.get()
  }

  @ManagedOperation(description = 'Reset all statistics')
  def resetAllStatus( )
  {
    requestsReceivedCount = new AtomicLong( 0 )
    responsesSentCount = new AtomicLong( 0 )
    errorsWithResponseSentCount = new AtomicLong( 0 )
    unrecognizedRequestsReceivedCount = new AtomicLong( 0 )
    lastServiceRequestTimestamp = new AtomicReference()
    lastServiceResponseTimestamp = new AtomicReference()
    lastServiceRequestInfo = new AtomicReference()
    areaAvailabilityRequestCount = new AtomicLong( 0 )
    singlePropertyAvailabilityRequestCount = new AtomicLong( 0 )
    ratePlanInfoRequestCount = new AtomicLong( 0 )
    newReservationRequestCount = new AtomicLong( 0 )
    modifyReservationRequestCount = new AtomicLong( 0 )
    cancelReservationRequestCount = new AtomicLong( 0 )
    ignoreReservationRequestCount = new AtomicLong( 0 )
    endTransactionRequestCount = new AtomicLong( 0 )
  }

}
