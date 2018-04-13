package com.walksocket.rc;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * tcp session.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public class RcSession {

  /**
   * default send timeout milliseconds.
   */
  private static final int DEFAULT_TIMEOUT_MILLISECONDS = 1500;

  /**
   * life timestamp milliseconds.
   */
  private long lifeTimestampMilliseconds;

  /**
   * idle milliseconds.
   */
  private int idleMilliSeconds = 60000;

  /**
   * session id.
   */
  private String sid;

  /**
   * socket channel.
   */
  private AsynchronousSocketChannel channel;

  /**
   * close queue.
   */
  private RcCloseQueue queue;

  /**
   * close handler called.
   */
  private boolean closeHandlerCalled = false;

  /**
   * local address.
   */
  private SocketAddress localAddress;

  /**
   * remote address.
   */
  private SocketAddress remoteAddress;

  /**
   * values.
   */
  private Map<String, Object> values;

  /**
   * or null.
   * @param f func
   * @param <T> type
   * @return value or null
   */
  private static <T> T orNull(Callable<T> f) {
    try {
      return f.call();
    } catch (Exception e) {
      RcLogger.debug(() -> e);
    }
    return null;
  }

  /**
   * constructor.
   * @param channel async socket channel
   */
  RcSession(AsynchronousSocketChannel channel) {
    this.sid = UUID.randomUUID().toString();
    this.channel = channel;
    this.localAddress = orNull(channel::getLocalAddress);
    this.remoteAddress = orNull(channel::getRemoteAddress);
  }

  /**
   * set queue.
   * @param queue close queue
   */
  void setQueue(RcCloseQueue queue) {
    this.queue = queue;
  }

  /**
   * send.
   * @param message your message
   * @throws RcSendException send error
   */
  public void send(byte[] message) throws RcSendException {
    send(message, DEFAULT_TIMEOUT_MILLISECONDS);
  }

  /**
   * send with timeout.
   * @param message your message
   * @param timeout timeout milliseconds
   * @throws RcSendException send error
   */
  public void send(byte[] message, long timeout) throws RcSendException {
    if (!isOpen()) {
      return;
    }
    synchronized (this) {
      try {
        channel.write(ByteBuffer.wrap(message)).get(timeout, TimeUnit.MILLISECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        RcLogger.error(e);

        // force close
        close();
        throw new RcSendException(e);
      }
    }
  }

  /**
   * close.
   */
  public void close() {
    if (!isOpen()) {
      return;
    }
    if (queue != null) {
      RcAttachmentRead attachmentRead = new RcAttachmentRead(
          channel,
          new RcCloseReason(RcCloseReason.Code.SELF_CLOSE));
      queue.add(attachmentRead);
    }
  }

  /**
   * is open.
   * @return if channel is open and close handler called false, true
   */
  public boolean isOpen() {
    return channel.isOpen() && !closeHandlerCalled;
  }

  /**
   * is close handler called.
   * @return if already called close handler, true
   */
  boolean isCloseHandlerCalled() {
    return closeHandlerCalled;
  }

  /**
   * close handler called.
   */
  void closeHandlerCalled() {
    closeHandlerCalled = true;
  }

  /**
   * get local address.
   * @return self address
   */
  public String getLocalAddress() {
    return localAddress.toString();
  }

  /**
   * get remote address.
   * @return peer address
   */
  public String getRemoteAddress() {
    return remoteAddress.toString();
  }

  /**
   * set value.
   * @param name your name
   * @param value your value
   */
  public void setValue(String name, Object value) {
    if (values == null) {
      values = new HashMap<>();
    }
    values.put(name, value);
  }

  /**
   * get value.
   * @param <T> your type
   * @param name your name
   * @param cls your class
   * @return your value or null
   */
  public <T> T getValue(String name, Class<T> cls) {
    if (values == null) {
      return null;
    }
    return cls.cast(values.get(name));
  }

  /**
   * get idle milliseconds.
   * @return idle milliseconds
   */
  public int getIdleMilliSeconds() {
    return idleMilliSeconds;
  }

  /**
   * set idle milliseconds.
   * @param idleMilliSeconds idle milliseconds
   */
  public void setIdleMilliSeconds(int idleMilliSeconds) {
    this.idleMilliSeconds = idleMilliSeconds;
  }

  /**
   * is timeout.
   * @return if timeout, true
   */
  boolean isTimeout() {
    return RcDate.timestampMilliseconds() > lifeTimestampMilliseconds;
  }

  /**
   * update timeout.
   */
  void updateTimeout() {
    lifeTimestampMilliseconds = RcDate.timestampMilliseconds() + idleMilliSeconds;
  }

  @Override
  public String toString() {
    return sid;
  }

  /**
   * send exception.
   * @author shigenobu
   *
   */
  public class RcSendException extends Exception {

    /**
     * version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * constructor.
     * @param e error
     */
    private RcSendException(Throwable e) {
      super(e);
    }
  }
}
