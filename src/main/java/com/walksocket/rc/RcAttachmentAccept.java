package com.walksocket.rc;

import java.nio.channels.AsynchronousServerSocketChannel;

/**
 * attachment accept.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public class RcAttachmentAccept {

  /**
   * server socket channel.
   */
  private AsynchronousServerSocketChannel channel;

  /**
   * constructor.
   * @param channel async server socket channel
   */
  RcAttachmentAccept(
      AsynchronousServerSocketChannel channel) {
    this.channel = channel;
  }

  /**
   * get channel.
   * @return async server socket channel
   */
  AsynchronousServerSocketChannel getChannel() {
    return channel;
  }

  @Override
  public String toString() {
    return String.format("RcAttachmentAccept - channel:%s", channel);
  }
}
