package com.walksocket.rc;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * shutdown handler.
 * @author shigenobu
 * @version 0.0.6
 *
 */
class RcShutdown implements Runnable {

  /**
   * in shutdown.
   */
  private final AtomicBoolean inShutdown = new AtomicBoolean(false);

  /**
   * in shutdown, custom executor.
   */
  private RcShutdownExecutor shutdownExecutor;

  /**
   * set custom executor.
   * @param shutdownExecutor custom executor.
   */
  void setExecutor(RcShutdownExecutor shutdownExecutor) {
    this.shutdownExecutor = shutdownExecutor;
  }

  /**
   * in shutdown.
   * @return if running shutdown, true
   */
  boolean inShutdown() {
    return inShutdown.get();
  }

  @Override
  public void run() {
    // start shutdown
    RcLogger.info("start shutdown handler");
    inShutdown.set(true);

    // sleep
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      RcLogger.error(e);
    }

    // execute
    if (shutdownExecutor != null) {
      shutdownExecutor.execute();
    }

    // end shutdown
    RcLogger.info("end shutdown handler");
  }
}
