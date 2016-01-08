package com.qozix.tileview.hotspots;

import android.graphics.Region;

public class HotSpot extends Region {

  private Object mTag;
  private HotSpotTapListener mHotSpotTapListener;

  public Object getTag() {
    return mTag;
  }

  public void setTag( Object object ) {
    mTag = object;
  }

  public void setHotSpotTapListener( HotSpotTapListener hotSpotTapListener ) {
    mHotSpotTapListener = hotSpotTapListener;
  }

  public HotSpotTapListener getHotSpotTapListener() {
    return mHotSpotTapListener;
  }

  public interface HotSpotTapListener {
    void onHotSpotTap( HotSpot hotSpot, int x, int y );
  }

  @Override
  public boolean equals( Object obj ) {
    if( obj instanceof HotSpot ) {
      HotSpot hotSpot = (HotSpot) obj;
      return super.equals( hotSpot ) && hotSpot.mHotSpotTapListener == mHotSpotTapListener;
    }
    return false;
  }
}
