package com.its.openpath.module.siteminder.handlers

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * <code>TakeActionOnStatusThread.groovy</code>
 * <p/>
 * Take Action on a given status
 * <p/>
 * @author Kent Fletcher
 * @since Sep 6, 2012
 */
public class TakeActionOnStatusThread
implements Runnable
{
  private static final Logger sLogger = LoggerFactory.getLogger( TakeActionOnStatusThread.class.name )
  
  Closure takeActionBeforeItsTooLate
  
  public TakeActionOnStatusThread(Closure c)
  {
    sLogger.info("Created....")
    this.takeActionBeforeItsTooLate = c
  }
  
  @Override
  public void run( )
  {
    try
    {
      if(sLogger.isDebugEnabled ())
        sLogger.debug("-> Checking transactions <-")
      takeActionBeforeItsTooLate.call()
    }
    catch (Throwable e)
    {
      sLogger.error("-> Checking for transactions <-", e)
    }
  }
}