package com.qozix.tileview.tiles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.animation.AnimationUtils;

import com.qozix.tileview.detail.DetailLevel;
import com.qozix.tileview.graphics.BitmapDecoder;

import java.util.List;

public class Tile {

	private int mWidth;
	private int mHeight;

	private int mRow;
	private int mColumn;

	private Object mData;

	private Bitmap mBitmap;

	private Rect mInputRect;
	private Rect mOutputRect;

	private int duration = 200;

	private Paint mPaint = new Paint();


  private List<Tile> mParentList;

	public double renderTimestamp;

  private boolean mTransitionsEnabled;

  private DetailLevel mDetailLevel;

  public Tile() {

  }

  public Tile( int column, int row, int width, int height, Object data, DetailLevel detailLevel ) {
    mRow = row;
    mColumn = column;
    mWidth = width;
    mHeight = height;
    mData = data;
    mDetailLevel = detailLevel;
  }

	public void stampTime(){
		// TODO: set a flag when completely rendered, and unset here, so we dont' have to recompute if we don't need to
		renderTimestamp = AnimationUtils.currentAnimationTimeMillis();
	}

  public void setTransitionsEnabled( boolean enabled ) {
    mTransitionsEnabled = enabled;
    // TODO: notify?
  }

  public DetailLevel getDetailLevel(){
    return mDetailLevel;
  }

	public float getRendered(){
    if( !mTransitionsEnabled ) {
      return 1;
    }
		double now = AnimationUtils.currentAnimationTimeMillis();
		double ellapsed = now - renderTimestamp;
    float progress = (float) Math.min(1, ellapsed / duration);
    // if it's transitioned in full, there won't be subsequent animations so stop computing
    if( progress == 1) {
      mTransitionsEnabled = false;
    }
		return progress;
	}

	public boolean getIsDirty(){
		return mTransitionsEnabled && getRendered() < 1f;
	}

	public Paint getPaint(){
    if( !mTransitionsEnabled ) {
      return null;
    }
		float rendered = getRendered();
		int opacity = (int) (rendered * 255);
		mPaint.setAlpha( opacity );
		return mPaint;
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
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

	public int getLeft(){
		return mColumn * mWidth;
	}

	public int getTop(){
		return mRow * mHeight;
	}

  public void setParentList( List<Tile> parentList ) {
    if(mParentList != null){
      if(mParentList.contains( this )){
        mParentList.remove( this );
      }
    }
    mParentList = parentList;
    if(mParentList!= null){
      if(!mParentList.contains( this )){
        mParentList.add( this );
      }
    }
  }



	public void decode( Context context, BitmapDecoder decoder ) {
		if (hasBitmap()) {
			return;
		}
		mBitmap = decoder.decode( this, context );
	}

  public void addToList( List<Tile> tiles ) {
    tiles.add( this );
    mParentList = tiles;
  }

  public void removeFromList( List<Tile> tiles ) {
    if( tiles.contains( this ) ){
      tiles.remove(this );
      mParentList = null;
    }
  }

	public void destroy() {
		mBitmap = null;
    setParentList( null );
    Log.d( "Tiles", "destroying tile at " + getLeft() + ", " + getTop() );
	}

	// TODO: measure bitmap here?
	public Rect getOutputRect(){
		if( mOutputRect == null){
			mOutputRect = new Rect();
			mOutputRect.top = getTop();
			mOutputRect.left = getLeft();
			mOutputRect.bottom = mOutputRect.top + getHeight();
			mOutputRect.right = mOutputRect.left + getWidth();
		}
		return mOutputRect;
	}

  /**
   *
   * @param canvas The canvas the tile's bitmap should be drawn into
   * @return True if the tile is dirty (drawing output has changed and needs parent validation)
   */
	public boolean draw( Canvas canvas ) {
    if( mBitmap != null ) {
      canvas.drawBitmap( mBitmap, getLeft(), getTop(), getPaint() );
    }
    return getIsDirty();
  }

	@Override
	public boolean equals( Object o ) {
		if ( o instanceof Tile) {
			Tile m = (Tile) o;
			return ( m.getRow() == getRow() )
					&& ( m.getColumn() == getColumn() )
					&& ( m.getWidth() == getWidth() )
					&& ( m.getHeight() == getHeight() )
					&& ( m.getDetailLevel() == getDetailLevel() );
		}
		return false;
	}

	@Override
	public String toString() {
		return "(row=" + mRow + ", column=" + mColumn + ", width=" + mWidth + ", height=" + mHeight + ", data=" + mData + ")";
	}

}
