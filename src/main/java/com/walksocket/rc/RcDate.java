package com.walksocket.rc;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * date utility.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public class RcDate {

  static {
    Locale.setDefault(Locale.ENGLISH);
  }

  /**
   * add milli seconds.
   */
  private static int addMilliSeconds = 0;

  /**
   * set add milli seconds.
   * <pre>
   *   if you want to set locale JP,
   *   call this method with 32400000(60 * 60 * 9 * 1000)
   * </pre>
   * @param addMilliSeconds milliseconds
   */
  public static void setAddMilliSeconds(int addMilliSeconds) {
    RcDate.addMilliSeconds = addMilliSeconds;
  }

  /**
   * get timestamp milliseconds.
   * @return timestamp millis
   */
  static long timestampMilliseconds() {
    Calendar cal = Calendar.getInstance();
    return cal.getTimeInMillis();
  }

  /**
   * get now.
   * <pre>
   *   default format yyyy-MM-dd HH:mm:ss.SSS
   * </pre>
   * @return now
   */
  static String now() {
    return now("yyyy-MM-dd HH:mm:ss.SSS");
  }

  /**
   * get now with format.
   * @param format simple date format pattern
   * @return now
   */
  static String now(String format) {
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    Date date = new Date();
    date.setTime(date.getTime() + addMilliSeconds);
    return sdf.format(date);
  }
}
