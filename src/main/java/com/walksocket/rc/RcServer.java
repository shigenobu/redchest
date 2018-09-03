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
 * @version 0.0.3
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
   * session close check devide number.
   */
  private int devide = 10;

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
   * session manager.
   */
  private RcSessionManager manager;

  /**
   * in shutdown, custom executor.
   */
  private RcShutdownExecutor executor;

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
   * set session check devide number.
   * @param devide session check devide number
   * @return this
   */
  public RcServer devide(int devide) {
    this.devide = devide;
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
   * set shutdown executor.
   * @param executor custom executor
   * @return this
   */
  public RcServer shutdownExecutor(RcShutdownExecutor executor) {
    this.executor = executor;
    return this;
  }

  /**
   * start.
   * @throws RcServerException server error
   */
  public void start() throws RcServerException {
    // set shutdown handler
    RcShutdown shutdown = new RcShutdown(executor);
    Thread shutdownThread = new Thread(shutdown);
    Runtime.getRuntime().removeShutdownHook(shutdownThread);
    Runtime.getRuntime().addShutdownHook(shutdownThread);

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
      manager = new RcSessionManager(devide, shutdown);
      manager.startServiceTimeout();

      // start server
      handler = new RcHandlerAccept(callback, readBufferSize, manager);
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
    if (manager != null) {
      manager.shutdownServiceTimeout();
    }

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
   * get session count.
   * @return session count.
   */
  public long getSessionCount() {
    if (manager != null) {
      return manager.getSessionCount();
    }
    return 0;
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
