package com.walksocket.rc;

import java.nio.channels.AsynchronousSocketChannel;

/**
 * attachment connect.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public class RcAttachmentConnect {

  /**
   * socket channel.
   */
  private AsynchronousSocketChannel channel;

  /**
   * constructor.
   * @param channel async socket channel
   */
  RcAttachmentConnect(
      AsynchronousSocketChannel channel) {
    this.channel = channel;
  }

  /**
   * get channel.
   * @return async socket channel
   */
  AsynchronousSocketChannel getChannel() {
    return channel;
  }

  @Override
  public String toString() {
    return String.format("RcAttachmentConnect - channel:%s", channel);
  }
}
