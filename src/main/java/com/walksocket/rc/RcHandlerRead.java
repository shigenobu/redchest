package com.walksocket.rc;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * read handler.
 * @author shigenobu
 * @version 0.0.7
 *
 */
class RcHandlerRead implements CompletionHandler<Integer, RcAttachmentRead> {

  /**
   * invalid read.
   */
  private static final int INVALID_READ = -1;

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
   * service close.
   */
  private ExecutorService serviceClose;

  /**
   * constructor.
   * @param callback callback when received
   * @param readBufferSize read buffer size
   * @param manager session manager
   */
  RcHandlerRead(RcCallback callback, int readBufferSize, RcSessionManager manager) {
    this.callback = callback;
    this.readBufferSize = readBufferSize;
    this.manager = manager;

    int num = Runtime.getRuntime().availableProcessors() / 4;
    if (num <= 0) {
      num = 1;
    }
    this.serviceClose = Executors.newFixedThreadPool(num);
    this.serviceClose.submit(() -> {
      while (true) {
        RcAttachmentRead attachmentRead = null;
        if ((attachmentRead = manager.getQueue().poll()) != null) {
          RcLogger.debug(String.format("close service - attachment:%s", attachmentRead));
          completed(INVALID_READ, attachmentRead);
        }
      }
    });
  }

  @Override
  public void completed(Integer result, RcAttachmentRead attachmentRead) {
    // attachment read
    AsynchronousSocketChannel channel = attachmentRead.getChannel();
    ByteBuffer buffer = attachmentRead.getBuffer();
    RcCloseReason reason = attachmentRead.getReason();

    // close
    if (result <= INVALID_READ) {
      try {
        channel.close();
      } catch (Exception e) {
        RcLogger.debug(() -> e);
      }

      // callback
      if (reason == null || reason.getCode() == RcCloseReason.Code.NONE) {
        reason = new RcCloseReason(RcCloseReason.Code.PEER_CLOSE);
      }
      RcSession session = manager.by(channel);
      if (session != null) {
        synchronized (session) {
          if (!session.isCloseHandlerCalled()) {
            session.closeHandlerCalled();
            callback.onClose(session, reason);
          }
        }
      }
      return;
    }

    // callback
    RcSession session = manager.get(channel);
    if (session != null) {
      byte[] message = new byte[result];
      // handling jdk8 with jdk9 build.
      ((Buffer) buffer).flip();
      buffer.get(message, 0, result);
      synchronized (session) {
        // if called close by self is false and timeout is false, true
        if (!session.isSelfClosed() && !session.isTimeout()) {
          session.updateTimeout();
          callback.onMessage(session, message);
        }
      }
    }

    // next
    ByteBuffer bufferNext = ByteBuffer.allocate(readBufferSize);
    RcAttachmentRead attachmentReadNext = new RcAttachmentRead(channel, bufferNext);
    if (channel.isOpen()) {
      channel.read(bufferNext, attachmentReadNext, this);
    }
  }

  @Override
  public void failed(Throwable e, RcAttachmentRead attachmentRead) {
    RcLogger.debug(() -> e);
    RcLogger.debug(() -> String.format("read failed - attachment:%s", attachmentRead));

    // attachment read
    AsynchronousSocketChannel channel = attachmentRead.getChannel();
    RcCloseReason reason = attachmentRead.getReason();

    // force close
    if (reason == null || reason.getCode() == RcCloseReason.Code.NONE) {
      reason = new RcCloseReason(RcCloseReason.Code.FAILED);
    }
    RcSession session = manager.by(channel);
    if (session != null) {
      synchronized (session) {
        if (!session.isCloseHandlerCalled()) {
          session.closeHandlerCalled();
          callback.onClose(session, reason);
        }
      }
    }
  }

  /**
   * shutdown.
   */
  void shutdown() {
    // shutdown service
    if (serviceClose != null) {
      serviceClose.shutdown();
      serviceClose = null;
    }
  }
}
