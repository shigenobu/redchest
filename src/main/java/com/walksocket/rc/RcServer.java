package com.walksocket.rc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * server.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public class RcServer {

  /**
   * callback.
   */
  private RcCallback callback;

  /**
   * source host.
   */
  private String host = "0.0.0.0";

  /**
   * source port.
   */
  private int port = 8710;

  /**
   * tcp backlog.
   */
  private int backlog = 1024;

  /**
   * read buffer size.
   */
  private int readBufferSize = 2048;

  /**
   * receive buffer size.
   */
  private int receiveBufferSize = 1024 * 1024 * 128;

  /**
   * pool.
   */
  private ExecutorService pool = Executors.newWorkStealingPool();

  /**
   * server socket channel.
   */
  private AsynchronousServerSocketChannel channel;

  /**
   * accept handler.
   */
  private RcHandlerAccept handler;

  /**
   * constructor.
   * @param callback callback when received
   */
  public RcServer(RcCallback callback) {
    this.callback = callback;
  }

  /**
   * set bind.
   * @param host destination host
   * @param port destination port
   * @return this
   */
  public RcServer bind(String host, int port) {
    this.host = host;
    this.port = port;
    return this;
  }

  /**
   * set backlog.
   * @param backlog tcp backlog
   * @return this
   */
  public RcServer backlog(int backlog) {
    this.backlog = backlog;
    return this;
  }

  /**
   * set read buffer size.
   * @param readBufferSize read buffer size
   * @return this
   */
  public RcServer readBufferSize(int readBufferSize) {
    this.readBufferSize = readBufferSize;
    return this;
  }

  /**
   * set receive buffer size.
   * <pre>
   *   set tcp rcv buffer.
   *   if under 0, depend on os settings.
   * </pre>
   * @param receiveBufferSize tcp rcv buffer size
   * @return this
   */
  public RcServer receiveBufferSize(int receiveBufferSize) {
    this.receiveBufferSize = receiveBufferSize;
    return this;
  }

  /**
   * set pool.
   * @param pool thread pool
   * @return this
   */
  public RcServer pool(ExecutorService pool) {
    this.pool = pool;
    return this;
  }

  /**
   * start.
   * @throws RcServerException server error
   */
  public void start() throws RcServerException {
    try {
      // init
      AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(pool);
      channel = AsynchronousServerSocketChannel.open(group);
      channel.bind(new InetSocketAddress(host, port), backlog);
      channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      if (receiveBufferSize > 0) {
        channel.setOption(StandardSocketOptions.SO_RCVBUF, receiveBufferSize);
      }

      // start service
      RcSessionManager.startServiceTimeout(RcSession.Owner.SERVER);

      // start server
      handler = new RcHandlerAccept(callback, readBufferSize);
      RcAttachmentAccept attachmentAccept = new RcAttachmentAccept(channel);
      channel.accept(attachmentAccept, handler);
      RcLogger.info(
          String.format(
              "server listen on %s:%s (backlog:%s, readBufferSize:%s, receiveBufferSize:%s, pool:%s)",
              host,
              port,
              backlog,
              readBufferSize,
              receiveBufferSize,
              pool));
    } catch (IOException e) {
      RcLogger.error(e);
      throw new RcServerException(e);
    }
  }

  /**
   * shutdown.
   */
  public void shutdown() {
    // shutdown service
    RcSessionManager.shutdownServiceTimeout();

    // close
    if (channel != null) {
      try {
        channel.close();
      } catch (IOException e) {
        RcLogger.error(e);
      }
    }
    if (handler != null) {
      handler.shutdown();
    }
  }

  /**
   * server exception.
   * @author furuta
   *
   */
  public class RcServerException extends Exception {

    /**
     * version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * constructor.
     * @param e error
     */
    private RcServerException(Throwable e) {
      super(e);
    }
  }
}
