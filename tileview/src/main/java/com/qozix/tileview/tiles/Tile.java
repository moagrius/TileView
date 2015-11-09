package com.qozix.tileview.tiles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.animation.AnimationUtils;

import com.qozix.tileview.detail.DetailLevel;
import com.qozix.tileview.graphics.BitmapProvider;

public class Tile {

  private static final int DEFAULT_TRANSITION_DURATION = 200;

  private int mWidth;
  private int mHeight;
  private int mLeft;
  private int mTop;

  private int mRow;
  private int mColumn;

  private Object mData;
  private Bitmap mBitmap;

  public double renderTimestamp;

  private boolean mTransitionsEnabled;

  private int mTransitionDuration = DEFAULT_TRANSITION_DURATION;

  private Paint mPaint;

  private TileCanvasView mParentTileCanvasView;

  private DetailLevel mDetailLevel;

  public Tile( int column, int row, int width, int height, Object data, DetailLevel detailLevel ) {
    mRow = row;
    mColumn = column;
    mWidth = width;
    mHeight = height;
    mLeft = column * width;
    mTop = row * height;
    mData = data;
    mDetailLevel = detailLevel;
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

  public void setTransitionDuration( int transitionDuration ) {
    mTransitionDuration = transitionDuration;
  }

  public void stampTime() {
    renderTimestamp = AnimationUtils.currentAnimationTimeMillis();
  }

  public void setTransitionsEnabled( boolean enabled ) {
    mTransitionsEnabled = enabled;
  }

  public DetailLevel getDetailLevel() {
    return mDetailLevel;
  }

  public float getRendered() {
    if( !mTransitionsEnabled ) {
      return 1;
    }
    double now = AnimationUtils.currentAnimationTimeMillis();
    double ellapsed = now - renderTimestamp;
    float progress = (float) Math.min( 1, ellapsed / mTransitionDuration );
    if( progress == 1 ) {
      mTransitionsEnabled = false;
    }
    return progress;
  }

  public boolean getIsDirty() {
    return mTransitionsEnabled && getRendered() < 1f;
  }

  public Paint getPaint() {
    if( !mTransitionsEnabled ) {
      return null;
    }
    if( mPaint == null ) {
      mPaint = new Paint();
    }
    float rendered = getRendered();
    int opacity = (int) (rendered * 255);
    mPaint.setAlpha( opacity );
    return mPaint;
  }

  void generateBitmap( Context context, BitmapProvider bitmapProvider ) {
    if( mBitmap != null ) {
      return;
    }
    mBitmap = bitmapProvider.getBitmap( this, context );
  }

  void setParentTileCanvasView( TileCanvasView tileCanvasView ) {
    mParentTileCanvasView = tileCanvasView;
  }

  void destroy( boolean shouldRecycle ) {
    if( shouldRecycle && mBitmap != null && !mBitmap.isRecycled() ) {
      mBitmap.recycle();
    }
    mBitmap = null;
    if( mParentTileCanvasView != null ) {
      mParentTileCanvasView.removeTile( this );
    }
  }

  /**
   * @param canvas The canvas the tile's bitmap should be drawn into
   * @return True if the tile is dirty (drawing output has changed and needs parent validation)
   */
  boolean draw( Canvas canvas ) {
    if( mBitmap != null ) {
      canvas.drawBitmap( mBitmap, mLeft, mTop, getPaint() );
    }
    return getIsDirty();
  }

  @Override
  public boolean equals( Object o ) {
    if( o instanceof Tile ) {
      Tile m = (Tile) o;
      return (m.getRow() == getRow())
        && (m.getColumn() == getColumn())
        && (m.getDetailLevel() == getDetailLevel());
    }
    return false;
  }

}
