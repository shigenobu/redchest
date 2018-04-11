package com.walksocket.rc;

/**
 * close reason.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public class RcCloseReason {

  /**
   * close reason code enum.
   * @author shigenobu
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
    TIMEOUT;
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
