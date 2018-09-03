package com.walksocket.rc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * connect handler.
 * @author shigenobu
 * @version 0.0.1
 *
 */
class RcHandlerConnect implements CompletionHandler<Void, RcAttachmentConnect> {

  /**
   * callback.
   */
  private RcCallback callback;

  /**
   * read buffer size.
   */
  private int readBufferSize;

  /**
   * session manager.
   */
  private RcSessionManager manager;

  /**
   * read handler.
   */
  private RcHandlerRead handler;

  /**
   * constructor.
   * @param callback callback when received
   * @param readBufferSize read buffer size
   * @param manager session manager
   */
  RcHandlerConnect(RcCallback callback, int readBufferSize, RcSessionManager manager) {
    this.callback = callback;
    this.readBufferSize = readBufferSize;
    this.manager = manager;
    this.handler = new RcHandlerRead(callback, readBufferSize, manager);
  }

  @Override
  public void completed(Void result, RcAttachmentConnect attachmentConnect) {
    // read attachment
    AsynchronousSocketChannel channel = attachmentConnect.getChannel();

    // running shutdown, new connection is abort
    if (RcShutdown.IN_SHUTDOWN.get()) {
      try {
        channel.close();
      } catch (IOException e) {
        RcLogger.debug(() -> e);
      }
      return;
    }

    // callback
    RcSession session = manager.generate(channel, new RcSession(channel));
    if (session != null) {
      synchronized (session) {
        session.updateTimeout();
        callback.onOpen(session);
      }
    }

    // read
    if (channel.isOpen()) {
      ByteBuffer buffer = ByteBuffer.allocate(readBufferSize);
      RcAttachmentRead attachmentRead = new RcAttachmentRead(channel, buffer);
      channel.read(buffer, attachmentRead, handler);
    }
  }

  @Override
  public void failed(Throwable e, RcAttachmentConnect attachmentConnect) {
    RcLogger.debug(() -> String.format("connect failed - exception:%s, attachment:%s", e, attachmentConnect));
  }

  /**
   * shutdown.
   */
  void shutdown() {
    handler.shutdown();
  }
}
