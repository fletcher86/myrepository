package com.its.openpath.module.opscommon.comm.bus.local

import static org.junit.Assert.*

import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.its.openpath.module.opscommon.comm.bus.IMessageBusSubscriber
import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import com.its.openpath.module.opscommon.util.TimeUUIDUtils

/**
 * <code>LocalMessageBusTest</code>
 * <p/>
 * Contains all Local Message Bus unit tests.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

class LocalMessageBusTest
{
  private Logger sLogger = LoggerFactory.getLogger( LocalMessageBusTest.class.name );


  /**
   * Test if a message can be published on a topic and received by multiple registered subscribers
   */
  @Test
  def void pubSubTest( )
  {
    OpsLocalMessageBus bus = new OpsLocalMessageBus()
    def subscriberId1 = "TestSubscriber1"
    def subscriberId2 = "TestSubscriber2"

    def subscriber1 = [
      getSubscriberId: {
        return subscriberId1
      },
      onMessage: {  String topic, OpsMessage message ->
        assert message
        assert message.data
        sLogger.info "${subscriberId1}, got it: ${message.data}, Thread id: ${Thread.currentThread().name} "
      }
    ] as IMessageBusSubscriber

    def subscriber2 = [
      getSubscriberId: {
        return subscriberId2
      },
      onMessage: {  String topic, OpsMessage responseMessage ->
        assertNotNull( "Response OpsMessage received is null", responseMessage )
        assert responseMessage.data
        sLogger.info "${subscriberId2}, got it: ${responseMessage.data}, Thread id: ${Thread.currentThread().name} "
      }
    ] as IMessageBusSubscriber

    def topicName = "test"
    bus.subscribeToTopic( topicName, subscriber1 )
    bus.subscribeToTopic( topicName, subscriber2 )

    UUID uuid = TimeUUIDUtils.getUniqueTimeUUIDinMicros ()
    OpsMessage message = new OpsMessage()
    message.messageType = 100
    message.correlationId = uuid.toString ()
    message.correlationIdBytes = TimeUUIDUtils.asByteString(uuid)
    message.timestamp = TimeUUIDUtils.getTimeFromUUID(uuid)
    message.data = "This is a test"
    bus.publishToTopic( topicName, message )
    sLogger.info "Published a test message on topic: [${topicName}]"
  }

  /**
   * Test if a message can be published on a topic, and if a response can be received synchronously on a specified
   * topic. Puts the calling Thread on hold till a response is received. The publish method returns 'false' if
   * a response didn't arrive on the response topic within the specified timeout.
   */
  @Test
  def void synchronousRequestReplyUsingQueueTest( )
  {
    OpsLocalMessageBus bus = new OpsLocalMessageBus()
    def requestQueueName = "testClosureReq"
    def replyQueueName = "testClosureRep"
    OpsMessage message = new OpsMessage()
    UUID uuid = TimeUUIDUtils.getUniqueTimeUUIDinMicros ()
    message.messageType = 100
    message.correlationId = uuid.toString ()
    message.correlationIdBytes = TimeUUIDUtils.asByteString ( uuid )
    message.timestamp = TimeUUIDUtils.getTimeFromUUID(uuid)
    message.data = "Request from the sender"

    // Create the request listener and consume from request queue
    def requestQueueListener = [
      getSubscriberId: {
        return "testCaseRequestListener"
      },
      onMessage: {  String topic, OpsMessage reqMessage ->
        reqMessage.data = 'Reply from the listener'
        bus.queueMessage(replyQueueName, reqMessage)
      }
    ] as IMessageBusSubscriber
    bus.consumeFromQueue(requestQueueName, requestQueueListener, false)

    sLogger.info "Sending a test message on queue: [${requestQueueName}]"
    boolean timedOut = bus.queueMessage( requestQueueName, replyQueueName, 5000, message ) { OpsMessage responseMessage ->
      assertNotNull( "Response OpsMessage received is null", responseMessage )
      assert responseMessage.data
      sLogger.info "Closure test Got it: ${responseMessage.data}, Thread id: ${Thread.currentThread().name} "
    }
    assertTrue "Timed out waiting for response: $timedOut on topic: ${replyQueueName}", timedOut
  }

}
