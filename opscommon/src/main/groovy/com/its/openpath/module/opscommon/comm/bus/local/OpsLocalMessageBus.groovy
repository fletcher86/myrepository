package com.its.openpath.module.opscommon.comm.bus.local

import com.google.common.eventbus.AsyncEventBus
import com.google.common.eventbus.Subscribe
import com.its.openpath.module.opscommon.comm.bus.IMessageBus
import com.its.openpath.module.opscommon.comm.bus.IMessageBusSubscriber
import com.its.openpath.module.opscommon.model.messaging.ops.OpsMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.PreDestroy

/**
 * <code>OpsLocalMessageBus</code>
 * <p/>
 * Provides message bus functionality within the local JVM. Uses Google's EventBus to provide point-to-point and
 * pub-sub functionality as defined in the {@link IMessageBus}
 * <p />
 * @see "http://code.google.com/p/guava-libraries/wiki/EventBusExplained"
 * @author rajiv@itstcb.com
 * @since May 2012
 */

@Service("OpsLocalMessageBus")
@ManagedResource('OPENPATH:name=/module/opscommon/messagebus/OpsLocalMessageBus')
class OpsLocalMessageBus
implements IMessageBus
{
  private static final String MAX_QUEUE_ENTRY_SIZE = 50

  private static final Logger sLogger = LoggerFactory.getLogger( OpsLocalMessageBus.class.name )

  private AtomicInteger mSubscriberIdCount = new AtomicInteger( 0 )
  private ExecutorService mExecutorService
  private Map<String, AsyncEventBusManager> mAsyncEventBusManagerMap = new ConcurrentHashMap<String, AsyncEventBusManager>()
  // Scheduler to use for cleaning up tmp queues
  public final ScheduledExecutorService mScheduler = Executors.newSingleThreadScheduledExecutor();


  /**
   * Constructor
   */
  OpsLocalMessageBus( )
  {
    mScheduler.scheduleWithFixedDelay( new QueueCleanupThread(), 0, 10, TimeUnit.SECONDS );
    mExecutorService = Executors.newCachedThreadPool();
    if ( mExecutorService instanceof ThreadPoolExecutor )
    {
      // If the bounded queue gets full silently discards the rejected tasks -- no messages are queued for emitting
      ((ThreadPoolExecutor) mExecutorService).setRejectedExecutionHandler( new ThreadPoolExecutor.DiscardPolicy() );
    }

    sLogger.info "Instantiated ..."
  }

  @ManagedAttribute(description = 'Count of queues, both persistent and temporary')
  def int getQueueCount( )
  {
    return mAsyncEventBusManagerMap.size()
  }

  /**
   * Called by the Spring Framework at Beanfactory shutdown time.
   */
  @PreDestroy
  def void destroy( )
  {
    mExecutorService.shutdown()
  }

  /**
   * @see {@link IMessageBus#queueMessage}
   */
  def boolean queueMessage( final String sendQueueName, final String receiveQueueName, int timeout,
    final OpsMessage message, final Closure callBackClosure )
  {
    AsyncEventBusManager asyncEventBusManager = mAsyncEventBusManagerMap.get( sendQueueName )
    if ( !asyncEventBusManager?.isQueue )
    {
      throw new RuntimeException( "Couldn't queue a message on Queue: [${sendQueueName}]; either a queue doesn't exist by that name, " +
        " or a Topic exists by that name" )
    }

    //TODO Need to pool Handler instances
    return new SynchronousQueueMessageHandler( sendQueueName, message, callBackClosure ).process( receiveQueueName, timeout )
  }

  /**
   * @see {@link IMessageBus#queueMessage}
   */
  def void queueMessage( final String sendQueueName, final OpsMessage message )
  {
    AsyncEventBusManager asyncEventBusManager = mAsyncEventBusManagerMap.get( sendQueueName )
    if ( !asyncEventBusManager?.isQueue )
    {
      throw new RuntimeException( "Couldn't queue a message on Queue: [${sendQueueName}]; either a queue doesn't exist by that name, " +
        " or a Topic exists by that name" )
    }
    if ( asyncEventBusManager == null )
    {
      mAsyncEventBusManagerMap.put( sendQueueName, asyncEventBusManager = new AsyncEventBusManager( sendQueueName, true, true ) )
    }
    asyncEventBusManager.post( message )
  }

  /**
   * @see {@link IMessageBus#consumeFromQueue
   */
  def consumeFromQueue( final String queueName, final IMessageBusSubscriber messageConsumer, boolean overrideCurrentConsumer )
  {
    AsyncEventBusManager asyncEventBusManager = null
    if ( (asyncEventBusManager = mAsyncEventBusManagerMap.get( queueName )) == null )
    {
      mAsyncEventBusManagerMap.put( queueName, asyncEventBusManager = new AsyncEventBusManager( queueName, true, true ) )
    }
    if ( !asyncEventBusManager.isQueue )
    {
      throw new RuntimeException( "Couldn't consume from Queue: [${queueName}]; a Topic exists by that name" )
    }
    if ( !overrideCurrentConsumer && asyncEventBusManager.registeredSubscriberCount > 0 )
    {
      IMessageBusSubscriber subscriber = asyncEventBusManager.getQueueConsumer()
      throw new IllegalStateException( "Couldn't register the consumer: [${messageConsumer} to consume from queue: [${queueName}], " +
        "the consumer: [${subscriber.subscriberId}] is already registered to dequeue" )
    }
    asyncEventBusManager.addOpsMessageBusSubscriber( messageConsumer )
  }

  /**
   * @see {@link IMessageBus#queueMessage}
   */
  def void publishToTopic( final String topic, final OpsMessage message )
  {
    AsyncEventBusManager asyncEventBusManager = null
    if ( (asyncEventBusManager = mAsyncEventBusManagerMap.get( topic )) == null )
    {
      mAsyncEventBusManagerMap.put( topic, asyncEventBusManager = new AsyncEventBusManager( topic, false, true ) )
    }
    if ( asyncEventBusManager.isQueue )
    {
      throw new RuntimeException( "Couldn't post on specified Topic: [${topic}]; a Queue exists by that name" )
    }

    asyncEventBusManager.post( message )
    if ( sLogger.debugEnabled )
    {
      sLogger.debug "Successfully published message on topic: [${topic}]"
    }
  }

  /**
   * @see {@link IMessageBus#subscribeToTopic}
   */
  def String subscribeToTopic( final String topic, final IMessageBusSubscriber messageSubscriber )
  {
    AsyncEventBusManager asyncEventBusManager = null
    if ( (asyncEventBusManager = mAsyncEventBusManagerMap.get( topic )) == null )
    {
      mAsyncEventBusManagerMap.put( topic, asyncEventBusManager = new AsyncEventBusManager( topic, false, true ) )
    }
    if ( asyncEventBusManager.isQueue )
    {
      throw new RuntimeException( "Couldn't subscribe to specified Topic: [${topic}]; a Queue exists by that name" )
    }
    asyncEventBusManager.addOpsMessageBusSubscriber( messageSubscriber )
    return "${topic}-${messageSubscriber.subscriberId}-${mSubscriberIdCount.getAndIncrement()}"
  }

  /**
   * @see {@link IMessageBus#unsubscribeFromTopic}
   */
  def void unsubscribeFromTopic( final String topic, final String subscriberId )
  {
    mAsyncEventBusManagerMap.get( topic )?.removeOpsMessageBusSubscriber( subscriberId )
  }


  /**
   * Inner class that wraps an {@link AsyncEventBus} instance and manages a List of all {@link IMessageBusSubscriber}
   * instances who are interested in receiving messages on the associated topic.
   */
  private class AsyncEventBusManager
  {
    boolean isQueue
    boolean isPersistent
    boolean isMessageConsumed = false
    private String mTopicName
    private AsyncEventBus mAsyncEventBus
    private List<IMessageBusSubscriber> mOpsMessageBusSubscriberList = new ArrayList<IMessageBusSubscriber>()


    /**
     * Constructor
     * <p />
     * @param topicName - Unique topic name associated
     * @param isQueue - TRUE if managing a Queue, else it's a Topic
     * @param persistent - TRUE = a persistent queue, not temp one
     */
    def AsyncEventBusManager( String topicName, boolean isQueue, boolean persistent )
    {
      mTopicName = topicName
      this.isQueue = isQueue
      this.isPersistent = persistent
      mAsyncEventBus = new AsyncEventBus( mExecutorService )
      mAsyncEventBus.register( this )
    }

    /**
     * Add the supplied subscriber to the internal Map. The subscriber will the invoked when messages are available
     * on the associated topic.
     * <p />
     * @param opsMessageBusSubscriber - Subscriber interested in receiving messages on the topic
     */
    def addOpsMessageBusSubscriber( IMessageBusSubscriber opsMessageBusSubscriber )
    {
      mOpsMessageBusSubscriberList.add( opsMessageBusSubscriber )
      if ( sLogger.debugEnabled )
      {
        sLogger.debug "Successfully added subscriber: [${opsMessageBusSubscriber.subscriberId}] to the topic: [${mTopicName}]"
      }
    }

    /**
     * Remove a subscriber identified by its Id from the internal Map.
     * <p />
     * @param subscriberId - Unique subscriber Id {@link OpsLocalMessageBus#subscribeToTopic}
     */
    def removeOpsMessageBusSubscriber( String subscriberId )
    {
      mOpsMessageBusSubscriberList.each {
        if ( it.subscriberId.equals( subscriberId ) )
        {
          mOpsMessageBusSubscriberList.remove( it )
          if ( sLogger.debugEnabled )
          {
            sLogger.debug "Successfully removed subscriber: [${subscriberId}] from the topic: [${mTopicName}]"
          }
        }
      }
    }

    /**
     * Return the currently registered count of subscribers on a Queue/Topic
     * <p />
     * @return int - subscriber count
     */
    def int getRegisteredSubscriberCount( )
    {
      mOpsMessageBusSubscriberList.size()
    }

    /**
     * Return the consumer registered on a queue.
     * <p />
     * @return IMessageBusSubscriber - the consumer instance
     */
    def IMessageBusSubscriber getQueueConsumer( )
    {
      mOpsMessageBusSubscriberList.get( 0 )
    }

    /**
     * Publish/post the supplied message on the topic
     * <p />
     * @param message - Message to publish
     */
    def post( OpsMessage message )
    {
      mAsyncEventBus.post( message )
    }

    /**
     * Invoked by the {@link AsyncEventBus} when messages arrive on the topic.
     * <p />
     * @param message - Message arrived on the topic
     */
    @Subscribe
    def onAsyncBusMessage( OpsMessage message )
    {
      // Dispatch the message to each registered subscriber
      mOpsMessageBusSubscriberList.each {
        it.onMessage( mTopicName, message )
      }
    }
  }

  /**
   * Helper class used to manage a synchronous request/reply interaction using a combination of a persistent and
   * temporary queue.
   */
  private class SynchronousQueueMessageHandler
  implements IMessageBusSubscriber
  {
    private static int sSubscriberIdx = 1
    private String mSubscriberId
    private String mRequestQueueName
    private OpsMessage mMessageToSend
    private Closure mCallbackClosure
    private AsyncEventBusManager mRequestQueueManager = null
    private AsyncEventBusManager mReplyQueueManager = null;
    final Lock lock = new ReentrantLock();
    final Condition responseReceived = lock.newCondition();


    /**
     * Constructor
     * <p />
     * @param requestQueueName - Unique name of the queue to send the request
     * @param messageToSend - request message to send
     * @param callbackClosure - Closure to call when the response arrives on the reply queue
     */
    def SynchronousQueueMessageHandler( String requestQueueName, OpsMessage messageToSend, Closure callbackClosure )
    {
      mSubscriberId = "MessageSubscriber-${sSubscriberIdx++} "
      mRequestQueueName = requestQueueName
      mMessageToSend = messageToSend
      mCallbackClosure = callbackClosure

      // Create the Request Queue if it doesn't exist
      if ( (mRequestQueueManager = mAsyncEventBusManagerMap.get( mRequestQueueName )) == null )
      {
        mAsyncEventBusManagerMap.put( mRequestQueueName, mRequestQueueManager = new AsyncEventBusManager( mRequestQueueName, true ) )
      }
    }

    /**
     * Create a temporary request queue to consume the response, post the associated OPS message to the request queue,
     * and wait for the response on the response queue.
     * <p />
     * @param replyQueueName - Unique name of the temporary reply queue
     * @param timeout - timeout (MS) to wait for the reply
     * @return boolean - FALSE == timed out waiting for the response on the temp reply queue
     */
    def boolean process( final String replyQueueName, int timeout )
    {
      try
      {
        lock.lock()
        // Crate the temp queue to consume the response and listen on it
        mReplyQueueManager = new AsyncEventBusManager( replyQueueName, true, false )
        mReplyQueueManager.addOpsMessageBusSubscriber( this )
        mAsyncEventBusManagerMap.put( replyQueueName, mReplyQueueManager )

        // Post to the request queue and wait for the response on the reply queue created above
        mRequestQueueManager.post( mMessageToSend )
        return responseReceived.await( timeout, TimeUnit.MILLISECONDS )
      }
      finally
      {
        lock.unlock()
      }
    }

    /**
     * @see {@link IMessageBusSubscriber#getSubscriberId}
     */
    String getSubscriberId( )
    {
      return mSubscriberId
    }

    /**
     * @see {@link IMessageBusSubscriber#onMessage}
     */
    void onMessage( final String topic, final OpsMessage message )
    {
      try
      {
        lock.lock()
        responseReceived.signal()
        mCallbackClosure.call( message )
        mReplyQueueManager.isMessageConsumed = true
      }
      finally
      {
        lock.unlock()
      }
    }
  }


  /**
   * The scheduler Thread that fires at pre-defined intervals. This iterates over the internal Map and removes temporary
   * queues.
   */
  private class QueueCleanupThread
  implements Runnable
  {
    @Override
    public void run( )
    {
      mAsyncEventBusManagerMap.each { String key, AsyncEventBusManager val ->
        if ( val.isMessageConsumed && !val.isPersistent )
        {
          mAsyncEventBusManagerMap.remove( key )
        }
      }
    }

  }

}
