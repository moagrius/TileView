package com.qozix.tileview.tiles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.animation.AnimationUtils;

import com.qozix.tileview.detail.DetailLevel;
import com.qozix.tileview.geom.FloatMathHelper;
import com.qozix.tileview.graphics.BitmapProvider;
import com.qozix.tileview.graphics.BitmapRecycler;

import java.lang.ref.WeakReference;

public class Tile {

  public enum State {
    UNASSIGNED,
    PENDING_DECODE,
    DECODED
  }

  private static final int DEFAULT_TRANSITION_DURATION = 200;

  private State mState = State.UNASSIGNED;

  private int mWidth;
  private int mHeight;
  private int mLeft;
  private int mTop;
  private int mRight;
  private int mBottom;

  private float mProgress;

  private int mRow;
  private int mColumn;

  private float mDetailLevelScale;

  private Object mData;
  private Bitmap mBitmap;

  private Rect mIntrinsicRect = new Rect();
  private Rect mBaseRect = new Rect();
  private Rect mRelativeRect = new Rect();
  private Rect mScaledRect = new Rect();

  public Long mRenderTimeStamp;

  private boolean mTransitionsEnabled;

  private int mTransitionDuration = DEFAULT_TRANSITION_DURATION;

  private Paint mPaint;

  private DetailLevel mDetailLevel;

  private WeakReference<TileRenderRunnable> mTileRenderRunnableWeakReference;
  private WeakReference<BitmapRecycler> mBitmapRecyclerReference;

  public Tile( int column, int row, int width, int height, Object data, DetailLevel detailLevel ) {
    mRow = row;
    mColumn = column;
    mWidth = width;
    mHeight = height;
    mLeft = column * width;
    mTop = row * height;
    mRight = mLeft + mWidth;
    mBottom = mTop + mHeight;
    mData = data;
    mDetailLevel = detailLevel;
    mDetailLevelScale = mDetailLevel.getScale();
    updateRects();
  }

  public int getWidth() {
    return mWidth;
  }

  public int getHeight() {
    return mHeight;
  }

  public int getLeft() {
    return mLeft;
  }

  public int getTop() {
    return mTop;
  }

  public int getRow() {
    return mRow;
  }

  public int getColumn() {
    return mColumn;
  }

  public Object getData() {
    return mData;
  }

  public Bitmap getBitmap() {
    return mBitmap;
  }

  public boolean hasBitmap() {
    return mBitmap != null;
  }

  public Rect getBaseRect() {
    return mBaseRect;
  }

  public Rect getRelativeRect() {
    return mRelativeRect;
  }

  /**
   * @deprecated
   * @return
   */
  public float getRendered() {
    return mProgress;
  }

  /**
   * @deprecated
   */
  public void stampTime() {
    // no op
  }

  public Rect getScaledRect( float scale ) {
    mScaledRect.set(
      (int) (mRelativeRect.left * scale),
      (int) (mRelativeRect.top * scale),
      (int) (mRelativeRect.right * scale),
      (int) (mRelativeRect.bottom * scale)
    );
    return mScaledRect;
  }

  private void updateRects() {
    mIntrinsicRect.set( 0, 0, mWidth, mHeight );
    mBaseRect.set( mLeft, mTop, mRight, mBottom );
    mRelativeRect.set(
      FloatMathHelper.unscale( mLeft, mDetailLevelScale ),
      FloatMathHelper.unscale( mTop, mDetailLevelScale ),
      FloatMathHelper.unscale( mRight, mDetailLevelScale ),
      FloatMathHelper.unscale( mBottom, mDetailLevelScale )
    );
    mScaledRect.set( mRelativeRect );
  }

  public void setTransitionDuration( int transitionDuration ) {
    mTransitionDuration = transitionDuration;
  }

  public State getState() {
    return mState;
  }

  public void setState( State state ) {
    mState = state;
  }

  public void execute( TileRenderPoolExecutor tileRenderPoolExecutor ) {
    execute( tileRenderPoolExecutor, null );
  }

  public void execute( TileRenderPoolExecutor tileRenderPoolExecutor, BitmapRecycler recycler ) {
    if(mState != State.UNASSIGNED){
      return;
    }
    mState = State.PENDING_DECODE;
    TileRenderRunnable runnable = new TileRenderRunnable();
    mTileRenderRunnableWeakReference = new WeakReference<>( runnable );
    mBitmapRecyclerReference = new WeakReference<>( recycler );
    runnable.setTile( this );
    runnable.setTileRenderPoolExecutor( tileRenderPoolExecutor );
    tileRenderPoolExecutor.execute( runnable );
  }

  public void computeProgress(){
    if( !mTransitionsEnabled ) {
      return;
    }
    if( mRenderTimeStamp == null ) {
      mRenderTimeStamp = AnimationUtils.currentAnimationTimeMillis();
      mProgress = 0;
      return;
    }
    double elapsed = AnimationUtils.currentAnimationTimeMillis() - mRenderTimeStamp;
    mProgress = (float) Math.min( 1, elapsed / mTransitionDuration );
    if( mProgress == 1f ) {
      mRenderTimeStamp = null;
      mTransitionsEnabled = false;
    }
  }

  public void setTransitionsEnabled( boolean enabled ) {
    mTransitionsEnabled = enabled;
    if( enabled ) {
      mProgress = 0f;
    }
  }

  public DetailLevel getDetailLevel() {
    return mDetailLevel;
  }

  public boolean getIsDirty() {
    return mTransitionsEnabled && mProgress < 1f;
  }

  public Paint getPaint() {
    if( !mTransitionsEnabled ) {
      return mPaint = null;
    }
    if( mPaint == null ) {
      mPaint = new Paint();
    }
    mPaint.setAlpha( (int) (255 * mProgress) );
    return mPaint;
  }

  void generateBitmap( Context context, BitmapProvider bitmapProvider ) {
    if( mBitmap != null ) {
      return;
    }
    mBitmap = bitmapProvider.getBitmap( this, context );
    mWidth = mBitmap.getWidth();
    mHeight = mBitmap.getHeight();
    mRight = mLeft + mWidth;
    mBottom = mTop + mHeight;
    updateRects();
    mState = State.DECODED;
  }

  /**
   * Deprecated
   * @param b
   */
  void destroy( boolean b ) {
    reset();
  }

  void reset() {
    if( mState == State.PENDING_DECODE ) {
      if ( mTileRenderRunnableWeakReference != null ) {
        TileRenderRunnable runnable = mTileRenderRunnableWeakReference.get();
        if( runnable != null ) {
          runnable.cancel( true );
        }
      }
    }
    mState = State.UNASSIGNED;
    mRenderTimeStamp = null;
    if( mBitmap != null ) {
      BitmapRecycler recycler = mBitmapRecyclerReference.get();
      if( recycler != null ) {
        recycler.recycleBitmap( mBitmap );
      }
    }
    mBitmap = null;
  }

  /**
   * @param canvas The canvas the tile's bitmap should be drawn into
   */
  public void draw( Canvas canvas ) {
    if( mBitmap != null && !mBitmap.isRecycled() ) {
      canvas.drawBitmap( mBitmap, mIntrinsicRect, mRelativeRect, getPaint() );
    }
  }

  @Override
  public int hashCode() {
    int hash = 17;
    hash = hash * 31 + getColumn();
    hash = hash * 31 + getRow();
    hash = hash * 31 + (int) (1000 * getDetailLevel().getScale());
    return hash;
  }

  @Override
  public boolean equals( Object o ) {
    if( this == o ) {
      return true;
    }
    if( o instanceof Tile ) {
      Tile m = (Tile) o;
      return m.getRow() == getRow()
        && m.getColumn() == getColumn()
        && m.getDetailLevel().getScale() == getDetailLevel().getScale();
    }
    return false;
  }

  public String toShortString(){
    return mColumn + ":" + mRow;
  }

}
