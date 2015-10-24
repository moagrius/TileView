package com.qozix.tileview.geom;

/**
 * @author Mike Dunn, 10/23/15.
 */
public class FloatMathHelper {
  public static int scale( int base, float multiplier ) {
    return (int) ((base * multiplier) + 0.5);
  }
  public static int unscale( int base, float multiplier ) {
    return (int) ((base / multiplier) + 0.5);
  }
}
