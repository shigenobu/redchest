package com.walksocket.rc;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * read handler.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public class RcHandlerRead implements CompletionHandler<Integer, RcAttachmentRead> {

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
   * service close.
   */
  private ExecutorService serviceClose;

  /**
   * constructor.
   * @param callback callback when received
   * @param readBufferSize read buffer size
   */
  RcHandlerRead(RcCallback callback, int readBufferSize) {
    this.callback = callback;
    this.readBufferSize = readBufferSize;

    int num = Runtime.getRuntime().availableProcessors() / 4;
    if (num <= 0) {
      num = 1;
    }
    this.serviceClose = Executors.newFixedThreadPool(num);
    this.serviceClose.submit(new Runnable() {

      @Override
      public void run() {
        while (true) {
          RcAttachmentRead attachmentRead = null;
          if ((attachmentRead = RcCloseQueue.poll()) != null) {
            completed(INVALID_READ, attachmentRead);
          }
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
      RcSession session = RcSessionManager.by(channel);
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
    RcSession session = RcSessionManager.get(channel);
    if (session != null) {
      byte[] message = new byte[result];
      buffer.flip();
      buffer.get(message, 0, result);
      synchronized (session) {
        session.updateTimeout();
        callback.onMessage(session, message);
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
    RcLogger.debug(() -> String.format("read failed - exception:%s, attachment:%s", e, attachmentRead));

    // attachment read
    AsynchronousSocketChannel channel = attachmentRead.getChannel();
    RcCloseReason reason = attachmentRead.getReason();

    // force close
    if (reason == null || reason.getCode() == RcCloseReason.Code.NONE) {
      reason = new RcCloseReason(RcCloseReason.Code.FAILED);
    }
    RcSession session = RcSessionManager.by(channel);
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
