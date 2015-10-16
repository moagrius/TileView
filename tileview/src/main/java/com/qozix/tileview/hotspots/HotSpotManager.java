package com.qozix.tileview.hotspots;

import android.graphics.Point;

import java.util.ArrayList;

public class HotSpotManager {

  private float mScale = 1;

  private ArrayList<HotSpotEventListener> mHotSpotEventListeners = new ArrayList<HotSpotEventListener>();
  private ArrayList<HotSpot> mHotSpots = new ArrayList<HotSpot>();

  public HotSpotManager() {
  }

  public float getScale() {
    return mScale;
  }

  public void setScale( float scale ) {
    mScale = scale;
  }

  public void addHotSpot( HotSpot hotSpot ) {
    mHotSpots.add( hotSpot );
  }

  public void removeHotSpot( HotSpot hotSpot ) {
    mHotSpots.remove( hotSpot );
  }

  public void addHotSpotEventListener( HotSpotEventListener listener ) {
    mHotSpotEventListeners.add( listener );
  }

  public void removeHotSpotEventListener( HotSpotEventListener listener ) {
    mHotSpotEventListeners.remove( listener );
  }

  public void clear() {
    mHotSpots.clear();
  }

  // work from end of list - match the last one added (equivalant to z-index)
  private HotSpot getMatch( Point point ) {
    Point scaledPoint = new Point();
    scaledPoint.x = (int) (point.x / mScale);
    scaledPoint.y = (int) (point.y / mScale);
    for( int i = mHotSpots.size() - 1; i >= 0; i-- ) {
      HotSpot hotSpot = mHotSpots.get( i );
      if( hotSpot.contains( scaledPoint.x, scaledPoint.y ) ) {
        return hotSpot;
      }
    }
    return null;
  }

  public void processHit( Point point ) {  // TODO: no points?
    // is there a match?
    HotSpot hotSpot = getMatch( point );
    if( hotSpot != null ) {
      HotSpotEventListener spotListener = hotSpot.getHotSpotEventListener();
      if( spotListener != null ) {
        spotListener.onHotSpotTap( hotSpot, point.x, point.y );
      }
      for( HotSpotEventListener listener : mHotSpotEventListeners ) {
        listener.onHotSpotTap( hotSpot, point.x, point.y );
      }
    }
  }

}
