package com.its.openpath.module.opscommon.comm.bus

import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage

/**
 * <code></code>
 * <p/>
 * <p/>
 * The interface that must be implemented to subscribe to the OPS Message Buss Communications Service and receive messages
 * published on specific topics.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

interface IMessageBusSubscriber
{
  /**
   * Return the unique id of this subscriber. The id can be anything, a class name for example, as long as it's unique.
   * The Message Bus Service uses this id to deliver messages to each registered subscriber.
   * <p/>
   * @return String - The unique subscriber Id
   */
  public String getSubscriberId( );

  /**
   * Invoked by the {@link com.its.openpath.module.opscommon.comm.bus.IMessageBus} implementation to deliver messages intended for this subscriber.
   * <p/>
   * @param topic - The name of the topic on which the message arrived
   * @param message - The message content
   * @throws RuntimeException - If the subscriber failed to process the message
   */
  public void onMessage( String topic, OpsMessage message )
  throws RuntimeException;

}
