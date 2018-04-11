package com.walksocket.rc;

import java.util.function.Supplier;

/**
 * logger for stdout.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public final class RcLogger {

  /**
   * verbose.
   */
  private static boolean verbose = false;

  /**
   * set vervose.
   * <pre>
   *   if true, log debug out to stdout.
   * </pre>
   * @param verbose if allowed debug level, set true
   */
  public static void setVerbose(boolean verbose) {
    RcLogger.verbose = verbose;
  }

  /**
   * loggin error level.
   * @param message
   */
  static void error(Object message) {
    out("E", message);
  }

  /**
   * logging info level.
   * @param message logging message
   */
  static void info(Object message) {
    out("I", message);
  }

  /**
   * logging debug level.
   * @param message logging message
   */
  static void debug(Supplier<Object> message) {
    if (!verbose) {
      return;
    }
    out("D", message.get());
  }

  /**
   * out.
   * @param level level name.
   * @param message logging message.
   */
  private static void out(String level, Object message) {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append(RcDate.now());
    builder.append("]");
    builder.append("[");
    builder.append("RC");
    builder.append("-");
    builder.append(level);
    builder.append("]");
    builder.append("[");
    builder.append(Thread.currentThread().getName());
    builder.append("]");
    builder.append(message.toString());
    if (message instanceof Throwable) {
      StackTraceElement[] stacks = ((Throwable) message).getStackTrace();
      for (StackTraceElement stack : stacks) {
        builder.append("\n");
        builder.append("(C:" + stack.getClassName() + ")");
        builder.append("(F:" + stack.getFileName() + ")");
        builder.append("(L:" + stack.getLineNumber() + ")");
        builder.append("(M:" + stack.getMethodName() + ")");
      }
    }
    System.out.println(builder.toString());
  }
}
