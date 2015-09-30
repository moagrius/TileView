package com.qozix.tileview.tiles;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;

import com.qozix.tileview.graphics.BitmapDecoder;

public class Tile {

	private int mWidth;
	private int mHeight;

	private int mRow;
	private int mColumn;

	private Object mData;

	private ImageView mImageView;
	private Bitmap mBitmap;

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

	public ImageView getImageView() {
		return mImageView;
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

	public Tile() {

	}

	public Tile( int column, int row, int width, int height, Object data ) {
		mRow = row;
		mColumn = column;
		mWidth = width;
		mHeight = height;
		mData = data;
	}


	public void decode( Context context, BitmapDecoder decoder ) {
		if (hasBitmap()) {
			return;
		}
		mBitmap = decoder.decode( this, context );
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
	}

	@Override
	public boolean equals( Object o ) {
		if ( o instanceof Tile) {
			Tile m = (Tile) o;
			return ( m.getRow() == getRow() )
					&& ( m.getColumn() == getColumn() )
					&& ( m.getWidth() == getWidth() )
					&& ( m.getHeight() == getHeight() )
					&& ( m.getData().equals( getData() ) );
		}
		return false;
	}

	@Override
	public String toString() {
		return "(row=" + mRow + ", column=" + mColumn + ", width=" + mWidth + ", height=" + mHeight + ", data=" + mData + ")";
	}

}
