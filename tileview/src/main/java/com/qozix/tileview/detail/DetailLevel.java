package com.qozix.tileview.detail;

import android.graphics.Rect;
import android.support.annotation.NonNull;

import com.qozix.tileview.tiles.Tile;

import java.util.HashSet;

public class DetailLevel implements Comparable<DetailLevel> {

  private float mScale;
  private Object mData;
  private int mTileWidth;
  private int mTileHeight;

  private DetailLevelManager mDetailLevelManager;
  private Rect mViewport = new Rect();

  private StateSnapshot mLastStateSnapshot;

  public DetailLevel( DetailLevelManager detailLevelManager, float scale, Object data, int tileWidth, int tileHeight ) {
    mDetailLevelManager = detailLevelManager;
    mScale = scale;
    mData = data;
    mTileWidth = tileWidth;
    mTileHeight = tileHeight;
  }

  public StateSnapshot computeState(){  // TODO: maybe re-compute state?

    double relativeScale = getRelativeScale();

    int drawableWidth = mDetailLevelManager.getScaledWidth();
    int drawableHeight = mDetailLevelManager.getScaledHeight();
    double offsetWidth = mTileWidth * relativeScale;
    double offsetHeight = mTileHeight * relativeScale;

    mViewport.set( mDetailLevelManager.getComputedViewport() );

    mViewport.top = Math.max( mViewport.top, 0 );
    mViewport.left = Math.max( mViewport.left, 0 );
    mViewport.right = Math.min( mViewport.right, drawableWidth );
    mViewport.bottom = Math.min( mViewport.bottom, drawableHeight );

    int startRow = (int) Math.floor( mViewport.top / offsetHeight );
    int endRow = (int) Math.ceil( mViewport.bottom / offsetHeight );
    int startColumn = (int) Math.floor( mViewport.left / offsetWidth );
    int endColumn = (int) Math.ceil( mViewport.right / offsetWidth );

    mLastStateSnapshot = new StateSnapshot( this, startRow, endRow, startColumn, endColumn );

    return mLastStateSnapshot;

  }

  public HashSet<Tile> calculateIntersections() {

    // must call computeState prior

    HashSet<Tile> intersections;
    // TODO: this?
    synchronized( this ){
      intersections = new HashSet<Tile>();
    }

    for( int currentRow = mLastStateSnapshot.startRow; currentRow < mLastStateSnapshot.endRow; currentRow++ ) {
      for( int currentColumn = mLastStateSnapshot.startColumn; currentColumn < mLastStateSnapshot.endColumn; currentColumn++ ) {
        Tile tile = new Tile( currentColumn, currentRow, mTileWidth, mTileHeight, mData, this );
        intersections.add( tile );
      }
    }

    return intersections;

  }

  public StateSnapshot getLastStateSnapshot(){
    return mLastStateSnapshot;
  }

  public float getScale() {
    return mScale;
  }

  public float getRelativeScale() {
    return mDetailLevelManager.getScale() / mScale;
  }

  public int getTileWidth() {
    return mTileWidth;
  }

  public int getTileHeight() {
    return mTileHeight;
  }

  public Object getData() {
    return mData;
  }

  public Rect getViewport(){
    return mViewport;
  }

  @Override
  public int compareTo( @NonNull DetailLevel detailLevel ) {
    return (int) Math.signum( getScale() - detailLevel.getScale() );
  }

  @Override
  public boolean equals( Object object ) {
    if( this == object ) {
      return true;
    }
    if( object instanceof DetailLevel ) {
      DetailLevel detailLevel = (DetailLevel) object;
      return (getScale() == detailLevel.getScale());
    }
    return false;
  }

  @Override
  public int hashCode() {
    long bits = (Double.doubleToLongBits( getScale() ) * 43);
    return (((int) bits) ^ ((int) (bits >> 32)));
  }

  public static class StateSnapshot {
    public int startRow;
    public int endRow;
    public int startColumn;
    public int endColumn;
    public DetailLevel detailLevel;
    public StateSnapshot( DetailLevel dl, int sr, int er, int sc, int ec ) {
      detailLevel = dl;
      startRow = sr;
      endRow = er;
      startColumn = sc;
      endColumn = ec;
    }
    public boolean equals( Object o ) {
      if( o == this ) {
        return true;
      }
      if( o instanceof StateSnapshot ) {
        StateSnapshot stateSnapshot = (StateSnapshot) o;
        return detailLevel.equals( stateSnapshot.detailLevel )
          && startRow == stateSnapshot.startRow
          && startColumn == stateSnapshot.startColumn
          && endRow == stateSnapshot.endRow
          && endColumn == stateSnapshot.endColumn;
      }
      return false;
    }
    public String toString(){
      return startRow + ", " + endRow + ", " + startColumn + ", " + endColumn;
    }
    // TODO: maybe need to add DetailLevel instance to this?
  }



}