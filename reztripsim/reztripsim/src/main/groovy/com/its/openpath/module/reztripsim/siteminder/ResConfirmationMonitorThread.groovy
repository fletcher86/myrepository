package com.its.openpath.module.reztripsim.siteminder

import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class ResConfirmationMonitorThread
implements Runnable
{
  private static final Logger sLogger = LoggerFactory.getLogger( ResConfirmationMonitorThread.class.name )
  
  Closure takeAction
  
  public ResConfirmationMonitorThread(Closure c)
  {
    sLogger.info("Created....")
    this.takeAction = c
  }
  
  @Override
  public void run( )
  {
    try
    {
      if(sLogger.isDebugEnabled ())
        sLogger.debug("-> Checking For Reservations to Confirm <-")
      takeAction?.call()
    }
    catch (Throwable e)
    {
      sLogger.error("-> Checking For Reservations to Confirm <-", e)
    }
  }
}