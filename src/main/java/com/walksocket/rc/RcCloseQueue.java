package com.walksocket.rc;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * close queue.
 * @author shigenobu
 * @version 0.0.1
 *
 */
class RcCloseQueue {

  /**
   * queue.
   */
  private ConcurrentLinkedQueue<RcAttachmentRead> queue = new ConcurrentLinkedQueue<>();

  /**
   * add.
   * @param attachmentRead attachment for close
   */
  void add(RcAttachmentRead attachmentRead) {
    queue.add(attachmentRead);
  }

  /**
   * poll.
   * @return attachment for close
   */
  RcAttachmentRead poll() {
    return queue.poll();
  }
}
