package com.qozix.utils;

public class Maths {

  public static final double LOG_2 = Math.log(2);

  public static double log2(int n) {
    return Math.log(n) / LOG_2;
  }

  public static int roundWithStep(float value, int step) {
    if (value == 0) {
      return 0;
    }
    return Math.round(value / step) * step;
  }

  public static int roundUpWithStep(float value, int step) {
    if (value == 0) {
      return 0;
    }
    return (int) Math.ceil(value / step) * step;
  }

  public static int roundDownWithStep(float value, int step) {
    if (value == 0) {
      return 0;
    }
    return (int) Math.floor(value / step) * step;
  }

  public static float divideSafely(int dividend, float divisor) {
    if (dividend == 0 || divisor == 0) {
      return 0;
    }
    return dividend / divisor;
  }

}
