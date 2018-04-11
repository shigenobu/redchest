package com.walksocket.rc;

/**
 * callback.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public interface RcCallback {

  /**
   * on open.
   * <pre>
   *   once called when completed connect for client.
   *   once called when completed accept for server.
   * </pre>
   * @param session tcp session
   */
  void onOpen(RcSession session);

  /**
   * on message.
   * <pre>
   *   any called when completed read for client.
   *   any called when completed read for server.
   * </pre>
   * @param session RcSession
   * @param message received data
   */
  void onMessage(RcSession session, byte[] message);

  /**
   * on close.
   * <pre>
   *   once called when completed close for client.
   *   once called when completed close for server.
   * </pre>
   * @param session tcp session
   * @param reason close reason
   */
  void onClose(RcSession session, RcCloseReason reason);
}
