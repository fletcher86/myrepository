package com.its.openpath.module.opscommon.util

/**
 * <code>OpsStatuses.groovy</code>
 * <p/>
 * This class encapsulates all know status types for Ops Transactions
 * <p/>
 * @author kent
 * @since Aug 28, 2012
 */
class OpsStatuses
{
  /**
   * SUCCESS
   */
  public static final String SUCCESS="success"
  
  /**
   *  FAILURE
   */
  public static final String FAILURE="failure"
  
  /**
   * IN PROGRESS
   */
  public static final String IN_PROGRESS="in.progress"
  
  /**
   * CONFIRM RESERVATIONS
   */
  public static final String CONFIRM_RESERVATIONS="confirm.reservations"
  /**
   * WAITING FOR CONFIRMED RESERVATIONS
   */
  public static final String WAITING_FOR_CONFIRMED_RESERVATIONS="waiting.for.reztrip.to.confirm.reservations"
  /**
   * CONFIRMED RESERVATIONS
   */
  public static final String CONFIRMED="confirmed.reservations"
}
