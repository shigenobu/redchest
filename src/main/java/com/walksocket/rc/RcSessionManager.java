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
public class RcSessionManager {

  /**
   * devide size.
   */
  private static final int SIZE = 10;

  /**
   * service timeout.
   */
  private static final ScheduledExecutorService serviceTimeout = Executors.newSingleThreadScheduledExecutor();

  /**
   * service no.
   */
  private static final AtomicInteger serviceNo = new AtomicInteger(0);

  /**
   * session locks.
   */
  private static final List<ReentrantLock> sessionLocks = new ArrayList<>(SIZE);

  static {
    for (int i = 0; i < SIZE; i++) {
      sessionLocks.add(new ReentrantLock());
    }
  }

  /**
   * sessions.
   */
  private static final List<ConcurrentHashMap<AsynchronousSocketChannel, RcSession>> sessions = new ArrayList<>(SIZE);

  static {
    for (int i = 0; i < SIZE; i++) {
      sessions.add(new ConcurrentHashMap<>());
    }
  }

  /**
   * session count.
   */
  private static final AtomicLong sessionCount = new AtomicLong(0);

  /**
   * get mod.
   * @param channel async socket channel.
   * @return mod channel and size
   */
  private static int getMod(AsynchronousSocketChannel channel) {
    int random = System.identityHashCode(channel);
    return Math.abs(random % SIZE);
  }

  /**
   * start service timeout.
   * @param owner client or server
   */
  static void startServiceTimeout(RcSession.Owner owner) {
    serviceTimeout.scheduleAtFixedRate(
        new Runnable() {

          @Override
          public void run() {
            int no = serviceNo.getAndIncrement();
            if (serviceNo.get() >= SIZE) {
              serviceNo.set(0);
            }
            try {
              sessionLocks.get(no).lock();
              for (Iterator<Map.Entry<AsynchronousSocketChannel, RcSession>> iterator
                      = sessions.get(no).entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<AsynchronousSocketChannel, RcSession> element = iterator.next();
                AsynchronousSocketChannel channel = element.getKey();
                RcSession session = element.getValue();
                if (session.isTimeout() && session.getOwner() == owner) {
                  RcAttachmentRead attachmentRead
                      = new RcAttachmentRead(
                          channel,
                          new RcCloseReason(RcCloseReason.Code.TIMEOUT));
                  RcCloseQueue.add(attachmentRead);
                }
              }
            } finally {
              sessionLocks.get(no).unlock();
            }
          }
        }, 1, 1, TimeUnit.SECONDS);
  }

  /**
   * shutdown service timeout.
   */
  static void shutdownServiceTimeout() {
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
  static RcSession generate(AsynchronousSocketChannel channel, RcSession session) {
    int mod = getMod(channel);
    if (!sessions.get(mod).containsKey(channel)) {
      try {
        sessionLocks.get(mod).lock();
        if (sessions.get(mod).putIfAbsent(channel, session) == null) {
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
  static RcSession get(AsynchronousSocketChannel channel) {
    int mod = getMod(channel);
    return sessions.get(mod).get(channel);
  }

  /**
   * by.
   * @param channel async socket channel
   * @return tcp session or null
   */
  static RcSession by(AsynchronousSocketChannel channel) {
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
  public static long getSessionCount() {
    return sessionCount.get();
  }
}
