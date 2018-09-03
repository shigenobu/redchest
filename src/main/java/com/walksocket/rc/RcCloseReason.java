package com.walksocket.rc;

/**
 * close reason.
 * @author shigenobu
 * @version 0.0.3
 *
 */
public class RcCloseReason {

  /**
   * close reason code enum.
   *
   */
  public enum Code {
    /**
     * none.
     */
    NONE,

    /**
     * peer.
     */
    PEER_CLOSE,

    /**
     * sefl.
     */
    SELF_CLOSE,

    /**
     * failed.
     */
    FAILED,

    /**
     * timeout.
     */
    TIMEOUT,

    /**
     * shutdown.
     */
    SHUTDOWN;
  }

  /**
   * close reason code.
   */
  private Code code;

  /**
   * constructor.
   * @param code close reason code
   */
  RcCloseReason(Code code) {
    this.code = code;
  }

  /**
   * get code.
   * @return close reason code
   */
  public Code getCode() {
    return code;
  }

  @Override
  public String toString() {
    return code.name();
  }
}
