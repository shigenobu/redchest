package com.walksocket.rc;

import java.util.concurrent.atomic.AtomicBoolean;

class RcShutdown implements Runnable {

  static final AtomicBoolean IN_SHUTDOWN = new AtomicBoolean(false);

  @Override
  public void run() {
    // start shutdown
    IN_SHUTDOWN.set(true);
  }
}
