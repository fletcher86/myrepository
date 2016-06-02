package com.its.openpath.module.opscommon.util

import com.dyuproject.protostuff.ByteString

/**
 * <code>TimeUUIDUtils.groovy</code>
 * <p/>
 * This is a wrapper class for com.netflix.astyanax.util.TimeUUIDUtils so that modules that are already
 * dependent on opscommon do not have to add another dependency
 * <p/>
 * @author kent
 * @since Jul 17, 2012
 */

class TimeUUIDUtils
{
  
  /**
   * Get the micros time in a long based on the UUID argument
   *<p />
   *@param arg  UUID
   *@return long
   */
  public static long getMicrosTimeFromUUID( UUID arg )
  {
    return com.netflix.astyanax.util.TimeUUIDUtils.getMicrosTimeFromUUID ( arg )
  }
  
  /**
   * get a new UUID based on a passed in timestamp.  Does not resolve duplicates
   *<p />
   *@param time
   *@return
   */
  public static UUID getMicrosTimeUUID( long time )
  {
    return com.netflix.astyanax.util.TimeUUIDUtils.getMicrosTimeUUID ( time )
  }
  
  public static long getTimeFromUUID( byte[] uuid )
  {
    return com.netflix.astyanax.util.TimeUUIDUtils.getTimeFromUUID ( uuid )
  }
  
  public static long getTimeFromUUID(ByteString uuid)
  {
    return com.netflix.astyanax.util.TimeUUIDUtils.getTimeFromUUID(uuid.toByteArray ())
  }
  
  /**
   * get the timestamp based on the the current thread local InvocationContext.context.correlationId
   * 
   *<p />
   *@return long
   */
  public static long getTimeFromUUID()
  {
    return getTimeFromUUID(InvocationContext.instance.correlationId)
  }
  
  public static long getTimeFromUUID( UUID uuid )
  {
    return com.netflix.astyanax.util.TimeUUIDUtils.getTimeFromUUID ( uuid )
  }
  
  public static UUID getTimeUUID( long time )
  {
    return com.netflix.astyanax.util.TimeUUIDUtils.getTimeUUID ( time )
  }
  
  /**
   * Gets a new and unique time uuid in microseconds based on the current timestamp.
   *<p />
   *@return
   */
  public static UUID getUniqueTimeUUIDinMicros()
  {
    return com.netflix.astyanax.util.TimeUUIDUtils.getUniqueTimeUUIDinMicros ()
  }
  
  /**
   * Gets a new and unique time uuid in milliseconds based on the current timestamp.
   *<p />
   *@return returns the time uuid in millis
   */
  public static UUID getUniqueTimeUUIDinMillis()
  {
    return com.netflix.astyanax.util.TimeUUIDUtils.getUniqueTimeUUIDinMillis ()
  }
  
  /**
   * Returns a UUID given a byte [] array.
   *<p />
   *@param uuid byte []
   *@return java.util.UUID
   */
  public static UUID toUUID(byte[] uuid)
  {
    return com.netflix.astyanax.util.TimeUUIDUtils.uuid(uuid, 0)
  }
  
  public static byte[] asByteArray(java.util.UUID uuid)
  {
    return com.netflix.astyanax.util.TimeUUIDUtils.asByteArray ( uuid )
  }
  
  /**
   * Convert uuid as a com.dyuproject.protostuff.ByteString for inclusion in proto object payloads
   *<p />
   *@return
   */
  public static ByteString asByteString(UUID uuid)
  {
    byte [] cid = TimeUUIDUtils.asByteArray(uuid)
    ByteString bs = ByteString.copyFrom(cid)
  }
}
