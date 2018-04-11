package com.walksocket.rc;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;

import org.junit.Test;

public class TestRcLogger {

  @Test
  public void testNormal() {
    RcLogger.info("test info");
    RcLogger.debug(() -> "test debug");

    try {
      throw new Exception("test normal exception");
    } catch (Exception e) {
      RcLogger.error(e);
    }
  }

  @Test
  public void testVerbose() {
    RcLogger.setVerbose(true);
    RcLogger.info("test info");
    RcLogger.debug(() -> "test debug");

    try {
      throw new Exception("test normal exception");
    } catch (Exception e) {
      RcLogger.error(e);
    }
    RcLogger.setVerbose(false);
  }
}
