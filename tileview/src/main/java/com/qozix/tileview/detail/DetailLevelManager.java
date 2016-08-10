package com.qozix.tileview.detail;

import android.graphics.Rect;

import com.qozix.tileview.geom.FloatMathHelper;

import java.util.Collections;
import java.util.LinkedList;

public class DetailLevelManager {

  protected LinkedList<DetailLevel> mDetailLevelLinkedList = new LinkedList<DetailLevel>();

  private DetailLevelChangeListener mDetailLevelChangeListener;

  protected float mScale = 1;

  private int mBaseWidth;
  private int mBaseHeight;
  private int mScaledWidth;
  private int mScaledHeight;

  private boolean mDetailLevelLocked;

  private int mPadding;

  private Rect mViewport = new Rect();
  private Rect mComputedViewport = new Rect();
  private Rect mComputedScaledViewport = new Rect();

  private DetailLevel mCurrentDetailLevel;

  public DetailLevelManager() {
    update();
  }

  public float getScale() {
    return mScale;
  }

  public void setScale( float scale ) {
    mScale = scale;
    update();
  }

  public int getBaseWidth() {
    return mBaseWidth;
  }

  public int getBaseHeight() {
    return mBaseHeight;
  }

  public int getScaledWidth() {
    return mScaledWidth;
  }

  public int getScaledHeight() {
    return mScaledHeight;
  }

  public void setSize( int width, int height ) {
    mBaseWidth = width;
    mBaseHeight = height;
    update();
  }

  public void setDetailLevelChangeListener( DetailLevelChangeListener detailLevelChangeListener ) {
    mDetailLevelChangeListener = detailLevelChangeListener;
  }

  /**
   * "pads" the viewport by the number of pixels passed.  e.g., setViewportPadding( 100 ) instructs the
   * DetailManager to interpret it's actual viewport offset by 100 pixels in each direction (top, left,
   * right, bottom), so more tiles will qualify for "visible" status when intersections are calculated.
   *
   * @param pixels The number of pixels to pad the viewport by.
   */
  public void setViewportPadding( int pixels ) {
    mPadding = pixels;
    updateComputedViewport();
  }

  public void updateViewport( int left, int top, int right, int bottom ) {
    mViewport.set( left, top, right, bottom );
    updateComputedViewport();
  }

  private void updateComputedViewport() {
    mComputedViewport.set( mViewport );
    mComputedViewport.top -= mPadding;
    mComputedViewport.left -= mPadding;
    mComputedViewport.bottom += mPadding;
    mComputedViewport.right += mPadding;
  }

  public Rect getViewport() {
    return mViewport;
  }

  public Rect getComputedViewport() {
    return mComputedViewport;
  }

  public Rect getComputedScaledViewport(float scale){
    mComputedScaledViewport.set(
      (int) (mComputedViewport.left * scale),
      (int) (mComputedViewport.top * scale),
      (int) (mComputedViewport.right * scale),
      (int) (mComputedViewport.bottom * scale)
    );
    return mComputedScaledViewport;
  }

  /**
   * While the detail level is locked (after this method is invoked, and before unlockDetailLevel is invoked),
   * the DetailLevel will not change, and the current DetailLevel will be scaled beyond the normal
   * bounds.  Normally, during any scale change the DetailManager searches for the DetailLevel with
   * a registered scale closest to the defined mScale.  While locked, this does not occur.
   */
  public void lockDetailLevel() {
    mDetailLevelLocked = true;
  }

  /**
   * Unlocks a DetailLevel locked with lockDetailLevel.
   */
  public void unlockDetailLevel() {
    mDetailLevelLocked = false;
  }

  public boolean getIsLocked() {
    return mDetailLevelLocked;
  }

  public void resetDetailLevels() {
    mDetailLevelLinkedList.clear();
    update();
  }

  public DetailLevel getCurrentDetailLevel() {
    return mCurrentDetailLevel;
  }

  protected void update() {
    boolean detailLevelChanged = false;
    if( !mDetailLevelLocked ) {
      DetailLevel matchingLevel = getDetailLevelForScale();
      if( matchingLevel != null ) {
        detailLevelChanged = !matchingLevel.equals( mCurrentDetailLevel );
        mCurrentDetailLevel = matchingLevel;
      }
    }
    mScaledWidth = FloatMathHelper.scale( mBaseWidth, mScale );
    mScaledHeight = FloatMathHelper.scale( mBaseHeight, mScale );
    if( detailLevelChanged ) {
      if( mDetailLevelChangeListener != null ) {
        mDetailLevelChangeListener.onDetailLevelChanged( mCurrentDetailLevel );
      }
    }
  }

  public void addDetailLevel( float scale, Object data, int tileWidth, int tileHeight ) {
    DetailLevel detailLevel = new DetailLevel( this, scale, data, tileWidth, tileHeight );
    if( mDetailLevelLinkedList.contains( detailLevel ) ) {
      return;
    }
    mDetailLevelLinkedList.add( detailLevel );
    Collections.sort( mDetailLevelLinkedList );
    update();
  }

  public DetailLevel getDetailLevelForScale() {
    if( mDetailLevelLinkedList.size() == 0 ) {
      return null;
    }
    if( mDetailLevelLinkedList.size() == 1 ) {
      return mDetailLevelLinkedList.get( 0 );
    }
    DetailLevel match = null;
    int index = mDetailLevelLinkedList.size() - 1;
    for( int i = index; i >= 0; i-- ) {
      match = mDetailLevelLinkedList.get( i );
      if( match.getScale() < mScale ) {
        if( i < index ) {
          match = mDetailLevelLinkedList.get( i + 1 );
        }
        break;
      }
    }
    return match;
  }

  public void invalidateAll(){
    for( DetailLevel detailLevel : mDetailLevelLinkedList ){
      detailLevel.invalidate();
    }
  }

  public interface DetailLevelChangeListener {
    void onDetailLevelChanged( DetailLevel detailLevel );
  }

}
