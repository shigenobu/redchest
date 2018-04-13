package com.walksocket.rc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * client.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public class RcClient {

  /**
   * callback.
   */
  private RcCallback callback;

  /**
   * destination host.
   */
  private String host;

  /**
   * destination port.
   */
  private int port;

  /**
   * read buffer size.
   */
  private int readBufferSize = 2048;

  /**
   * socket channel.
   */
  private AsynchronousSocketChannel channel;

  /**
   * connect handler.
   */
  private RcHandlerConnect handler;

  /**
   * session manager.
   */
  private RcSessionManager manager;

  /**
   * constructor.
   * @param callback callback when received
   * @param host destination host
   * @param port destination port
   */
  public RcClient(RcCallback callback, String host, int port) {
    this.callback = callback;
    this.host = host;
    this.port = port;
  }

  /**
   * set read buffer size.
   * @param readBufferSize read buffer size
   * @return this
   */
  public RcClient readBufferSize(int readBufferSize) {
    this.readBufferSize = readBufferSize;
    return this;
  }

  /**
   * connect.
   * @throws RcClientException client error
   */
  public void connect() throws RcClientException {
    try {
      // open
      channel = AsynchronousSocketChannel.open();

      // start service
      manager  = new RcSessionManager(1);
      manager.startServiceTimeout();

      // connect
      handler = new RcHandlerConnect(callback, readBufferSize, manager);
      RcAttachmentConnect attachmentConnect = new RcAttachmentConnect(channel);
      channel.connect(new InetSocketAddress(host, port), attachmentConnect, handler);
      RcLogger.info(
          String.format(
              "connect to %s:%s (readBufferSize:%s)",
              host,
              port,
              readBufferSize));
    } catch (IOException e) {
      RcLogger.error(e);
      throw new RcClientException(e);
    }
  }

  /**
   * disconnect.
   */
  public void disconnect() {
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
   * client exception.
   * @author shigenobu
   *
   */
  public class RcClientException extends Exception {

    /**
     * version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * constructor.
     * @param e error
     */
    private RcClientException(Throwable e) {
      super(e);
    }
  }
}
