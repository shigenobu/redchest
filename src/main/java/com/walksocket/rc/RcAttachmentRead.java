package com.walksocket.rc;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * attachment read.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public class RcAttachmentRead {

  /**
   * socket channel.
   */
  private AsynchronousSocketChannel channel;

  /**
   * buffer.
   */
  private ByteBuffer buffer;

  /**
   * close reason.
   */
  private RcCloseReason reason;

  /**
   * constructor.
   * @param channel async socket channel
   */
  RcAttachmentRead(
      AsynchronousSocketChannel channel) {
    this(
        channel,
        ByteBuffer.allocate(0),
        new RcCloseReason(RcCloseReason.Code.NONE));
  }

  /**
   * constructor.
   * @param channel async socket channel
   * @param buffer received data
   */
  RcAttachmentRead(
      AsynchronousSocketChannel channel,
      ByteBuffer buffer) {
    this(
        channel,
        buffer,
        new RcCloseReason(RcCloseReason.Code.NONE));
  }

  /**
   * constructor.
   * @param channel async socket channel
   * @param reason close reason
   */
  RcAttachmentRead(
      AsynchronousSocketChannel channel,
      RcCloseReason reason) {
    this(
        channel,
        ByteBuffer.allocate(0),
        reason);
  }

  /**
   * constructor.
   * @param channel async socket channel
   * @param buffer received data
   * @param reason close reason
   */
  RcAttachmentRead(
      AsynchronousSocketChannel channel,
      ByteBuffer buffer,
      RcCloseReason reason) {
    this.channel = channel;
    this.buffer = buffer;
    this.reason = reason;
  }

  /**
   * get channel.
   * @return async socket channel
   */
  AsynchronousSocketChannel getChannel() {
    return channel;
  }

  /**
   * get buffer.
   * @return received data
   */
  ByteBuffer getBuffer() {
    return buffer;
  }

  /**
   * get reason.
   * @return close reason
   */
  RcCloseReason getReason() {
    return reason;
  }

  @Override
  public String toString() {
    return String.format("RcAttachmentRead - channel:%s, buffer:%s, reason:%s", channel, buffer, reason);
  }
}
