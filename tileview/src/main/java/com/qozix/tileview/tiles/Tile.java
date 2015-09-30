package com.qozix.tileview.tiles;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;

import com.qozix.tileview.graphics.BitmapDecoder;

public class Tile {

	private int mWidth;
	private int mHeight;

	private int mRow;
	private int mCol;

	private Object mData;

	private ImageView mImageView;
	private Bitmap mBitmap;

	private boolean mHasBitmap;

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	public int getRow() {
		return mRow;
	}

	public int getCol() {
		return mCol;
	}

	public Object getData() {
		return mData;
	}

	public ImageView getImageView() {
		return mImageView;
	}

	public Bitmap getBitmap() {
		return mBitmap;
	}

	public boolean isHasBitmap() {
		return mHasBitmap;
	}

	public Tile() {

	}

	public Tile( int row, int col, int width, int height, Object data ) {
		mRow = row;
		mCol = col;
		mWidth = width;
		mHeight = height;
		mData = data;
	}


	public void decode( Context context, BitmapDecoder decoder ) {
		if (mHasBitmap) {
			return;
		}
		mBitmap = decoder.decode( this, context );
		Log.d("DEBUG", mBitmap != null ? mBitmap.toString() : "mBitmap is null" );
		mHasBitmap = ( mBitmap != null );

	}

	public void render( Context context ) {
		if ( mImageView == null ) {
			mImageView = new ImageView( context );
			mImageView.setAdjustViewBounds( false );
			mImageView.setScaleType( ImageView.ScaleType.MATRIX );
		}		
		mImageView.setImageBitmap( mBitmap );
	}

	public void destroy() {
		if ( mImageView != null ) {
			mImageView.clearAnimation();
			mImageView.setImageBitmap( null );
			ViewParent parent = mImageView.getParent();
			if ( parent != null && parent instanceof ViewGroup) {
				ViewGroup group = (ViewGroup) parent;
				group.removeView( mImageView );
			}
			mImageView = null;
		}
		mBitmap = null;
		mHasBitmap = false;
	}

	@Override
	public boolean equals( Object o ) {
		if ( o instanceof Tile) {
			Tile m = (Tile) o;
			return ( m.getRow() == getRow() )
					&& ( m.getCol() == getCol() )
					&& ( m.getWidth() == getWidth() )
					&& ( m.getHeight() == getHeight() )
					&& ( m.getData().equals( getData() ) );
		}
		return false;
	}

	@Override
	public String toString() {
		return "(row=" + mRow + ", col=" + mCol + ", mWidth=" + mWidth + ", mHeight=" + mHeight + ", file=" + mData + ")";
	}

}
