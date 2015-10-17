package com.qozix.tileview.detail;

import android.graphics.Rect;
import android.support.annotation.NonNull;

import com.qozix.tileview.tiles.Tile;

import java.util.LinkedList;

public class DetailLevel implements Comparable<DetailLevel> {

  private float mScale;
  private Object mData;
  private int mTileWidth;
  private int mTileHeight;

  private DetailLevelManager mDetailLevelManager;
  private Rect mViewport = new Rect();

  public DetailLevel( DetailLevelManager detailLevelManager, float scale, Object data, int tileWidth, int tileHeight ) {
    mDetailLevelManager = detailLevelManager;
    mScale = scale;
    mData = data;
    mTileWidth = tileWidth;
    mTileHeight = tileHeight;
  }

  public LinkedList<Tile> calculateIntersections() {

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

    int startingRow = (int) Math.floor( mViewport.top / offsetHeight );
    int endingRow = (int) Math.ceil( mViewport.bottom / offsetHeight );
    int startingColumn = (int) Math.floor( mViewport.left / offsetWidth );
    int endingColumn = (int) Math.ceil( mViewport.right / offsetWidth );

    LinkedList<Tile> intersections = new LinkedList<Tile>();

    for( int iterationRow = startingRow; iterationRow < endingRow; iterationRow++ ) {
      for( int iterationColumn = startingColumn; iterationColumn < endingColumn; iterationColumn++ ) {
        Tile tile = new Tile( iterationColumn, iterationRow, mTileWidth, mTileHeight, mData, this );
        intersections.add( tile );
      }
    }

    return intersections;

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

  @Override
  public int compareTo( @NonNull DetailLevel detailLevel ) {
    return (int) Math.signum( getScale() - detailLevel.getScale() );
  }

  @Override
  public boolean equals( Object object ) {
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

}