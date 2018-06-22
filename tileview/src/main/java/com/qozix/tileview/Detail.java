package com.qozix.tileview;

import com.qozix.utils.Maths;

/**
 * ZOOM     PERCENT     SAMPLE
 * 0        100%        1
 * 1        50%         2
 * 2        25%         4
 * 3        12.5%       8
 * 4        6.25%       16
 * 5        3.125%      32
 * ...
 * handy math:
 * get percent from zoom: (1 >> zoom) / 1f (number of digits is precision, so (100 >> zoom) / 100f will give .012
 * get percent from sample: 1 / sample
 * get sample from zoom: 1 << zoom
 * get zoom from sample: Maths.log2(sample)
 */

public class Detail {

  public static int getZoomFromPercent(float percent) {
    return (int) Maths.log2((int) (1 / percent));
  }

  // this sample is the sample size for grid computation, not image sampling
  private int mSample;
  private int mZoom;
  private Object mData;

  public Detail(int zoom, Object data) {
    mData = data;
    mZoom = zoom;
    mSample = 1 << zoom;
  }

  public Object getData() {
    return mData;
  }

  public int getZoom() {
    return mZoom;
  }

  public int getSample() {
    return mSample;
  }

  @Override
  public int hashCode() {
    int hash = 17;
    hash = hash * 31 + mZoom;
    hash = hash * 31 + mData.hashCode();
    return hash;
  }
}
