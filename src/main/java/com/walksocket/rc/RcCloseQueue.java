package com.walksocket.rc;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * close queue.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public class RcCloseQueue {

  /**
   * queue.
   */
  private static ConcurrentLinkedQueue<RcAttachmentRead> queue = new ConcurrentLinkedQueue<>();

  /**
   * add.
   * @param attachmentRead attachment for close
   */
  static void add(RcAttachmentRead attachmentRead) {
    queue.add(attachmentRead);
  }

  /**
   * poll.
   * @return attachment for close
   */
  static RcAttachmentRead poll() {
    return queue.poll();
  }
}
