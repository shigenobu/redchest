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
 * @version 0.0.1
 *
 */
class RcSessionManager {

  /**
   * session close check devide number.
   */
  private int devide;

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
   */
  RcSessionManager(int devide) {
    this.devide = devide;
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
            int no = serviceNo.getAndIncrement();
            if (serviceNo.get() >= devide) {
              serviceNo.set(0);
            }
            try {
              sessionLocks.get(no).lock();
              for (Iterator<Map.Entry<AsynchronousSocketChannel, RcSession>> iterator
                      = sessions.get(no).entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<AsynchronousSocketChannel, RcSession> element = iterator.next();
                AsynchronousSocketChannel channel = element.getKey();
                RcSession session = element.getValue();
                if (session.isTimeout()) {
                  RcAttachmentRead attachmentRead
                      = new RcAttachmentRead(
                          channel,
                          new RcCloseReason(RcCloseReason.Code.TIMEOUT));
                  queue.add(attachmentRead);
                }
              }
            } finally {
              sessionLocks.get(no).unlock();
            }
          }
        }, start, offset, TimeUnit.MILLISECONDS);
  }

  /**
   * shutdown service timeout.
   */
  void shutdownServiceTimeout() {
    if (!serviceTimeout.isShutdown()) {
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
      try {
        sessionLocks.get(mod).lock();
        if (sessions.get(mod).putIfAbsent(channel, session) == null) {
          session.setQueue(queue);
          sessionCount.incrementAndGet();
        }
      } finally {
        sessionLocks.get(mod).unlock();
      }
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
      try {
        sessionLocks.get(mod).lock();
        if ((session = sessions.get(mod).remove(channel)) != null) {
          sessionCount.decrementAndGet();
        }
      } finally {
        sessionLocks.get(mod).unlock();
      }
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
}
