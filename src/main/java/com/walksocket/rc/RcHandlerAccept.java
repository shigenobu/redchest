package com.walksocket.rc;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * accept handler.
 * @author shigenobu
 * @version 0.0.1
 *
 */
class RcHandlerAccept implements CompletionHandler<AsynchronousSocketChannel, RcAttachmentAccept> {

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
  RcHandlerAccept(RcCallback callback, int readBufferSize, RcSessionManager manager) {
    this.callback = callback;
    this.readBufferSize = readBufferSize;
    this.manager = manager;
    this.handler = new RcHandlerRead(callback, readBufferSize, manager);
  }

  @Override
  public void completed(AsynchronousSocketChannel channel, RcAttachmentAccept attachmentAccept) {
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

    // next
    attachmentAccept.getChannel().accept(attachmentAccept, this);
  }

  @Override
  public void failed(Throwable e, RcAttachmentAccept attachmentAccept) {
    RcLogger.debug(() -> String.format("accept failed - exception:%s, attachment:%s", e, attachmentAccept));
  }

  /**
   * shutdown.
   */
  void shutdown() {
    handler.shutdown();
  }
}
