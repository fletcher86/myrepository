package com.its.openpath.module.opscommon.util

import com.dyuproject.protostuff.ByteString

/**
 * <code>InvocationContext</code>
 * <p/>
 * Represents contextual that pertains to method invocations. This can act as a container that can store data elements
 * and state that can passed passed around method invocations or stored a a Threadlocal variable.
 * <p />
 * @author rajiv@itstcb.com
 * @since May 2012
 */

class InvocationContext
{
  /** Threadlocal instance returned by the static factory method **/
  private static ThreadLocal<InvocationContext> sTreadLocalInstance = new ThreadLocal<InvocationContext>()

  /** Unique Correlation ID of the current session/transaction **/
  UUID correlationId
  
  /** The same correlation ID above represented as a com.dyuproject.protostuff.ByteString **/
  ByteString correlationIdByteString

  /** Service Request data received from an external entity **/
  Object externalSystemRequestData

  /** Service Response data that must be sent back to the external entity */
  Object externalSystemResponseData

  /** Service Request data that must be passed to a OpenPath Module **/
  Object openPathRequestData

  /** Service Response data that was received from an OpenPath Module **/
  Object openPathResponseData

  /** Additional arbitrary data element about the current session/transaction **/
  private Map<String, Object> sessionDataMap = new HashMap<String, Object>()

  /** Processing state of the current session/transaction **/
  boolean success = true



  /**
   * Constructor
   * <p />
   * @param correlationId - Unique correlation ID
   */
  private def InvocationContext( UUID correlationId )
  {
    this.correlationId = correlationId
    this.correlationIdByteString = TimeUUIDUtils.asByteString ( correlationId )
  }

  /**
   * Factory method that creates a new instance, stores it as a Threadlocal, and returns the reference.
   * <p />
   * @param correlationId - Unique correlation ID
   * @return InvocationContext - Instance created
   */
  def static InvocationContext getNewInstance( UUID correlationId )
  {
    def instance = new InvocationContext( correlationId )
    sTreadLocalInstance.set( instance )
    return getInstance()
  }

  /**
   * Factory method that returns the current Threadlocal instance.
   * <p />
   * @return InvocationContext - Current ThreadLocal instance
   */
  def static InvocationContext getInstance( )
  {
    sTreadLocalInstance.get()
  }

  /**
   * Add the provided object with the given key to the internal Map.
   * <p />
   * @param key - Unique key to use
   * @param object - Object reference
   */
  def void addSessionDataItem( String key, Object object )
  {
    this.sessionDataMap.put( key, object )
  }

  /**
   * Return a previously stored data item by the given key.
   * <p />
   * @param key - Unique key
   * @return - Object reference
   */
  def Object getSessionDataItem( String key )
  {
    this.sessionDataMap.get( key )
  }

}
