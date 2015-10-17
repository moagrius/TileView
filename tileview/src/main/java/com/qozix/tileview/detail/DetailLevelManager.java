package com.qozix.tileview.detail;

import android.graphics.Rect;

import java.util.Collections;
import java.util.LinkedList;

public class DetailLevelManager {

	private LinkedList<DetailLevel> mDetailLevelLinkedList = new LinkedList<DetailLevel>();

	private DetailLevelChangeListener mDetailLevelChangeListener;

	private float mScale = 1;

	private int mBaseWidth;
	private int mBaseHeight;
	private int mScaledWidth;
	private int mScaledHeight;

	private boolean mDetailLevelLocked = false;

	private int mPadding = 0;

	private Rect mViewport = new Rect();
	private Rect mComputedViewport = new Rect();

	private DetailLevel mCurrentDetailLevel;

	public DetailLevelManager() {
		update( true );
	}

	public float getScale() {
		return mScale;
	}

	public void setScale( float scale ) {
		boolean changed = (mScale != scale);
		mScale = scale;
		update( changed );
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
		update( true );
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

	private void update( boolean changed ) {
		// has there been a change in tile sets?
		boolean detailLevelChanged = false;
		// if detail level is locked, do not change tile sets
		if( !mDetailLevelLocked ) {  // TODO: check changed here?
			// get the most appropriate detail level for the current mScale
			DetailLevel matchingLevel = getDetailLevelForScale();
			// if one is found (if any tile sets are registered)
			if( matchingLevel != null ) {
				// is it the same as the one being used?  // TODO: why equals?
				detailLevelChanged = !matchingLevel.equals( mCurrentDetailLevel );
				// update current detail level
				mCurrentDetailLevel = matchingLevel;
			}
		}
		// update scaled values
		mScaledWidth = (int) (getBaseWidth() * getScale());
		mScaledHeight = (int) (getBaseHeight() * getScale());
		// if there's a change in detail, update appropriate values
		if( detailLevelChanged ) {
			// notify all interested parties
			if( mDetailLevelChangeListener != null ) {
				mDetailLevelChangeListener.onDetailLevelChanged( mCurrentDetailLevel );
			}
		}
	}

	public void lockDetailLevel() {
		mDetailLevelLocked = true;
	}

	public void unlockDetailLevel() {
		mDetailLevelLocked = false;
	}

	public void addDetailLevel( float scale, Object data, int tileWidth, int tileHeight ) {
		DetailLevel detailLevel = new DetailLevel( this, scale, data, tileWidth, tileHeight );
		if( mDetailLevelLinkedList.contains( detailLevel ) ) {
			return;
		}
		mDetailLevelLinkedList.add( detailLevel );
		Collections.sort( mDetailLevelLinkedList );
		update( false );
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

	public void resetDetailLevels() {
		mDetailLevelLinkedList.clear();
		update( false );
	}

	public DetailLevel getCurrentDetailLevel() {
		return mCurrentDetailLevel;
	}

	public interface DetailLevelChangeListener {
		void onDetailLevelChanged( DetailLevel detailLevel );
	}

}
