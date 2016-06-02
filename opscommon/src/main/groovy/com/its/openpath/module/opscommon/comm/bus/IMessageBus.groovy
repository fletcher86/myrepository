package com.its.openpath.module.opscommon.comm.bus

import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage

/**
 * <code>IMessageBus</code>
 * <p/>
 * Defines a generic Message Bus Service provided by the OPS Framework. Provides a pub/sub mechanism
 * to exchange messages between entities, typically between modules.
 * <p/>
 * Messages 'published' on named topics and are delivered to all registered subscribers. Messages 'queued' are
 * consumed by a single single listener.
 * <p/>
 * Note: All publish and queue operations are asynchronous, i.e, a new internal Thread is used to publish the message instead of
 * the calling Thread.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

interface IMessageBus
{
  /**
   * Queue the supplied message on a queue specified its name and expect a response on the specified Queue Name. This is a
   * synchronous method, as such, the calling Thread is blocked until a response is received on the specified topic
   * or if the timeout has reached.
   * <p/>
   * @param sendQueueName - Unique queue name to publish the message
   * @param receiveQueueName - Unique queue name to expect a response
   * @param timeout - Timeout (MS) to wait for the response
   * @param message - Message to be queued
   * @param callBackClosure - Closure to invoke to handle the response
   * @return boolean - FALSE == if timed out waiting for the response
   * @throws RuntimeException - If the message cannot be queued
   */
  def boolean queueMessage( final String sendQueueName, final String receiveQueueName, int timeout,
    final OpsMessage message, final Closure callBackClosure )

  /**
   * Queue the supplied message on a queue specified its name. This is fire-and-forget style.
   * <p/>
   * @param sendQueueName - Unique queue name to publish the message
   * @param message - Message to be queued
   * @throws RuntimeException - If the message cannot be queued
   */
  def void queueMessage( final String sendQueueName, final OpsMessage message )
  throws RuntimeException

  /**
   * Consume messages from a queue identified by the given name. Typically there can only be one consumer registered to
   * dequeue messages from the queue, however the 'overrideCurrentConsumer' can be used to override a currently registered
   * consumer and register a new one.
   * <p/>
   * @param queueName - Unique queue name to consume messages from
   * @param messageConsumer - Queue consumer reference to invoke when messages arrive on the queue
   * @throws RuntimeException - if cannot register subscriber to consume messages
   */
  def consumeFromQueue( final String queueName, final IMessageBusSubscriber messageConsumer, boolean overrideCurrentConsumer )
  throws RuntimeException

  /**
   * Publish the supplied message on the specified Topic. All subscribers on the topic will receive the message.
   * <p/>
   * @param topic - Unique topic name
   * @param message - Message to be published
   * @throws RuntimeException - If the message cannot be published
   */
  public void publishToTopic( String topic, OpsMessage message )
  throws RuntimeException;

  /**
   * Subscribe on the specified Topic to receive messages.
   * <p/>
   * @param topic - Unique topic name
   * @param messageSubscriber - The callback to invoke to deliver messages
   * @return String - Unique subscriber id assigned by the Service
   * @throws RuntimeException - If the subscribe operation failed
   */
  public String subscribeToTopic( final String topic, final IMessageBusSubscriber messageSubscriber )
  throws RuntimeException;

  /**
   * Un-subscribe from the specified Topic.
   * <p/>
   * @param topic - Unique topic name
   * @param subscriberId - The unique subscriber id assigned
   * @throws RuntimeException - If the un-subscribe operation failed
   */
  public void unsubscribeFromTopic( final String topic, final String subscriberId )
  throws RuntimeException;

}
