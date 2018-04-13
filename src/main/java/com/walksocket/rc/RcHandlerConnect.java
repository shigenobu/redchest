package com.walksocket.rc;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * connect handler.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public class RcHandlerConnect implements CompletionHandler<Void, RcAttachmentConnect> {

  /**
   * callback.
   */
  private RcCallback callback;

  /**
   * read buffer size.
   */
  private int readBufferSize;

  /**
   * read handler.
   */
  private RcHandlerRead handler;

  /**
   * constructor.
   * @param callback callback when received
   * @param readBufferSize read buffer size
   */
  RcHandlerConnect(RcCallback callback, int readBufferSize) {
    this.callback = callback;
    this.readBufferSize = readBufferSize;
    this.handler = new RcHandlerRead(callback, readBufferSize);
  }

  @Override
  public void completed(Void result, RcAttachmentConnect attachmentConnect) {
    // read attachment
    AsynchronousSocketChannel channel = attachmentConnect.getChannel();

    // callback
    RcSession session = RcSessionManager.generate(channel, new RcSession(channel, RcSession.OWner.CLIENT));
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
