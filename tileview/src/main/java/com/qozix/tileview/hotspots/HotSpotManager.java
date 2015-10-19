package com.qozix.tileview.hotspots;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

public class HotSpotManager {

  private float mScale = 1;

  private HashSet<HotSpot.HotSpotTapListener> mHotSpotTapListeners = new HashSet<HotSpot.HotSpotTapListener>();
  private LinkedList<HotSpot> mHotSpots = new LinkedList<HotSpot>();

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

  public void addHotSpotTapListener( HotSpot.HotSpotTapListener listener ) {
    mHotSpotTapListeners.add( listener );
  }

  public void removeHotSpotTapListener( HotSpot.HotSpotTapListener listener ) {
    mHotSpotTapListeners.remove( listener );
  }

  public void clear() {
    mHotSpots.clear();
  }

  private HotSpot getMatch( int x, int y ) {
    int scaledX = (int) (x / mScale + 0.5);
    int scaledY = (int) (y / mScale + 0.5);
    Iterator<HotSpot> iterator = mHotSpots.descendingIterator();
    while(iterator.hasNext()){
      HotSpot hotSpot = iterator.next();
      if( hotSpot.contains( scaledX, scaledY ) ) {
        return hotSpot;
      }
    }
    return null;
  }

  public void processHit( int x, int y ) {
    // is there a match?
    HotSpot hotSpot = getMatch( x, y );
    if( hotSpot != null ) {
      HotSpot.HotSpotTapListener spotListener = hotSpot.getHotSpotTapListener();
      if( spotListener != null ) {
        spotListener.onHotSpotTap( hotSpot, x, y );
      }
      for( HotSpot.HotSpotTapListener listener : mHotSpotTapListeners ) {
        listener.onHotSpotTap( hotSpot, x, y );
      }
    }
  }

}
