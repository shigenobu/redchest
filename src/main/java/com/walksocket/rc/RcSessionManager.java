package com.walksocket.rc;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * session manager.
 * @author shigenobu
 * @version 0.0.7
 *
 */
class RcSessionManager {

  /**
   * session close check devide number.
   */
  private int devide;

  /**
   * shutdown handler.
   */
  private RcShutdown shutdown;

  /**
   * service timeout.
   */
  private final ScheduledExecutorService serviceTimeout = Executors.newSingleThreadScheduledExecutor();

  /**
   * service no.
   */
  private final AtomicInteger serviceNo = new AtomicInteger(0);

  /**
   * session locks.
   */
  private final List<ReentrantLock> sessionLocks;

  /**
   * sessions.
   */
  private final List<ConcurrentHashMap<AsynchronousSocketChannel, RcSession>> sessions;

  /**
   * session count.
   */
  private final AtomicLong sessionCount = new AtomicLong(0);

  /**
   * queue.
   */
  private RcCloseQueue queue;

  /**
   * constructor
   * @param devide session close check devide number
   * @param shutdown shutdown handler
   */
  RcSessionManager(int devide, RcShutdown shutdown) {
    this.devide = devide;
    this.shutdown = shutdown;
    this.sessionLocks = new ArrayList<>(devide);
    for (int i = 0; i < devide; i++) {
      sessionLocks.add(new ReentrantLock());
    }
    this.sessions = new ArrayList<>(devide);
    for (int i = 0; i < devide; i++) {
      sessions.add(new ConcurrentHashMap<>());
    }
    this.queue = new RcCloseQueue();
  }

  /**
   * get queue.
   * @return close queue.
   */
  RcCloseQueue getQueue() {
    return queue;
  }

  /**
   * get mod.
   * @param channel async socket channel.
   * @return mod channel and size
   */
  private int getMod(AsynchronousSocketChannel channel) {
    int random = System.identityHashCode(channel);
    return Math.abs(random % devide);
  }

  /**
   * start service timeout.
   */
  void startServiceTimeout() {
    int start = 1000 / devide;
    int offset = 1000 / devide;
    serviceTimeout.scheduleAtFixedRate(
        new Runnable() {

          @Override
          public void run() {
            // if running shutdown, current connetions are force to close
            if (shutdown.inShutdown()) {
              RcLogger.info(String.format("in shutdown, left session count:%s", getSessionCount()));
              for (int i = 0; i < devide; i++) {
                sessionLocks.get(i).lock();
                sessions.get(i).forEach((channel, session) -> {
                  synchronized (session) {
                    // if called close by self is false and shutdown handler is not called, true
                    if (!session.isSelfClosed() && !session.isShutdownHandlerCalled()) {
                      session.shutdownHandlerCalled();
                      RcAttachmentRead attachmentRead
                          = new RcAttachmentRead(
                              channel,
                              new RcCloseReason(RcCloseReason.Code.SHUTDOWN));
                      queue.add(attachmentRead);
                    }
                  }
                });
                sessionLocks.get(i).unlock();
              }
              return;
            }

            // if session was timeout, connections are force to close
            int no = serviceNo.getAndIncrement();
            if (serviceNo.get() >= devide) {
              serviceNo.set(0);
            }
            sessionLocks.get(no).lock();
            sessions.get(no).forEach((channel, session) -> {
              synchronized (session) {
                // if called close by self is false and timeout is true, true
                if (!session.isSelfClosed() && session.isTimeout()) {
                  RcAttachmentRead attachmentRead
                      = new RcAttachmentRead(
                      channel,
                      new RcCloseReason(RcCloseReason.Code.TIMEOUT));
                  queue.add(attachmentRead);
                }
              }
            });
            sessionLocks.get(no).unlock();
          }
        }, start, offset, TimeUnit.MILLISECONDS);
  }

  /**
   * shutdown service timeout.
   */
  void shutdownServiceTimeout() {
    if (!serviceTimeout.isShutdown()) {
      RcLogger.info(String.format("in force shutdown, left session count:%s", getSessionCount()));
      for (int i = 0; i < devide; i++) {
        sessionLocks.get(i).lock();
        sessions.get(i).forEach((channel, session) -> {
          synchronized (session) {
            // if called close by self is false and shutdown handler is not called, true
            if (!session.isSelfClosed() && !session.isShutdownHandlerCalled()) {
              session.shutdownHandlerCalled();
              RcAttachmentRead attachmentRead
                  = new RcAttachmentRead(
                  channel,
                  new RcCloseReason(RcCloseReason.Code.SHUTDOWN));
              queue.add(attachmentRead);
            }
          }
        });
        sessionLocks.get(i).unlock();
      }
      serviceTimeout.shutdown();
    }
  }

  /**
   * generate.
   * @param channel async socket channel
   * @param session tcp session
   * @return tcp session
   */
  RcSession generate(AsynchronousSocketChannel channel, RcSession session) {
    int mod = getMod(channel);
    if (!sessions.get(mod).containsKey(channel)) {
      sessionLocks.get(mod).lock();
      if (sessions.get(mod).putIfAbsent(channel, session) == null) {
        session.setQueue(queue);
        sessionCount.incrementAndGet();
      }
      sessionLocks.get(mod).unlock();
    }
    return sessions.get(mod).get(channel);
  }

  /**
   * get.
   * @param channel async socket channel
   * @return tcp session
   */
  RcSession get(AsynchronousSocketChannel channel) {
    int mod = getMod(channel);
    return sessions.get(mod).get(channel);
  }

  /**
   * by.
   * @param channel async socket channel
   * @return tcp session or null
   */
  RcSession by(AsynchronousSocketChannel channel) {
    int mod = getMod(channel);
    RcSession session = null;
    if (sessions.get(mod).containsKey(channel)) {
      sessionLocks.get(mod).lock();
      if ((session = sessions.get(mod).remove(channel)) != null) {
        sessionCount.decrementAndGet();
      }
      sessionLocks.get(mod).unlock();
    }
    return session;
  }

  /**
   * get session count.
   * @return session count
   */
  long getSessionCount() {
    return sessionCount.get();
  }

  /**
   * get shutdown.
   * @return shutdown handler.
   */
  RcShutdown getShutdown() {
    return shutdown;
  }
}
