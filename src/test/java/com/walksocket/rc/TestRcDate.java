package com.walksocket.rc;

import org.junit.jupiter.api.Test;

import java.util.TimeZone;

public class TestRcDate {

  @Test
  public void testTimezone() {
    long now = RcDate.timestampMilliseconds();

    RcDate.setAddMilliSeconds(32400000);
    long nowJp1 = RcDate.timestampMilliseconds();
    System.out.println(nowJp1);

    RcDate.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
    long nowJp2 = RcDate.timestampMilliseconds();
    System.out.println(nowJp2);

    System.out.println(RcDate.now());
  }
}
