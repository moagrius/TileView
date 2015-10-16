package com.qozix.tileview.detail;

import android.graphics.Rect;

import com.qozix.tileview.tiles.selector.TileSetSelector;
import com.qozix.tileview.tiles.selector.TileSetSelectorMinimalUpScale;

import java.util.HashSet;

public class DetailLevelManager {

	private static final float PRECISION = 6;
	private static final double DECIMAL = Math.pow( 10, PRECISION );
	
	private DetailLevelSet mDetailLevels = new DetailLevelSet();
	private HashSet<DetailLevelEventListener> mDetailLevelEventListeners = new HashSet<DetailLevelEventListener>();
	private HashSet<DetailLevelSetupListener> mDetailLevelSetupListeners = new HashSet<DetailLevelSetupListener>();

	private float mScale = 1;
	private float mHistoricalScale;
	
	private DetailLevel mCurrentDetailLevel;
	
	private int mWidth;
	private int mHeight;
	private int mScaledWidth;
	private int mScaledHeight;
	
	private boolean mDetailLevelLocked = false;
	
	private int mPadding = 0;
	private Rect mViewport = new Rect();
	private Rect mComputedViewport = new Rect();


	private static float getAtPrecision( float s ) {
		return (float) (Math.round( s * DECIMAL ) / DECIMAL);
	}

	public DetailLevelManager(){
		update( true );
	}

	public float getScale() {
		return mScale;
	}

	public void setScale( float s ) {
		// round to PRECISION decimal places
		// DEBUG: why are we rounding still?
		s = getAtPrecision( s );
		// is it changed?
		boolean changed = ( mScale != s );
		// set it
		mScale = s;
		// update computed values
		update( changed );		
	}
	
	public int getWidth(){
		return mWidth;
	}
	
	public int getHeight(){
		return mHeight;
	}
	
	// DEBUG: needed?  maybe use ZPL's mWidth and mHeight...?
	public int getScaledWidth(){
		return mScaledWidth;
	}
	
	public int getScaledHeight(){
		return mScaledHeight;
	}
	
	public void setSize( int w, int h ) {
		mWidth = w;
		mHeight = h;
		update( true );
	}
	
	/**
	 *  "pads" the mViewport by the number of pixels passed.  e.g., setPadding( 100 ) instructs the
	 *  DetailManager to interpret it's actual mViewport offset by 100 pixels in each direction (top, left,
	 *  right, bottom), so more tiles will qualify for "visible" status when intersections are calculated.
	 * @param pixels (int) the number of pixels to pad the mViewport by
	 */
	public void setPadding( int pixels ) {
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
	
	private void update( boolean changed ){
		// has there been a change in tile sets?
		boolean detailLevelChanged = false;		
		// if detail level is locked, do not change tile sets
		if(!mDetailLevelLocked ){
			// get the most appropriate detail level for the current mScale
			DetailLevel matchingLevel = mDetailLevels.find( getScale() );
			// if one is found (if any tile sets are registered)
			if(matchingLevel != null){
				// is it the same as the one being used?
				detailLevelChanged = !matchingLevel.equals( mCurrentDetailLevel );
				// update current detail level
				mCurrentDetailLevel = matchingLevel;
			}			
		}		
		// update scaled values
		mScaledWidth = (int) ( getWidth() * getScale() );
		mScaledHeight = (int) ( getHeight() * getScale() );
		// broadcast mScale change
		if( changed ) {
			for ( DetailLevelEventListener listener : mDetailLevelEventListeners ) {
				listener.onDetailScaleChanged( getScale() );
			}			
		}
		// if there's a change in detail, update appropriate values
		if ( detailLevelChanged ) {			
			// notify all interested parties
			for ( DetailLevelEventListener listener : mDetailLevelEventListeners ) {
				listener.onDetailLevelChanged( mCurrentDetailLevel );
			}
		}
	}

	public void lockDetailLevel(){
		mDetailLevelLocked = true;
	}
	
	public void unlockDetailLevel(){
		mDetailLevelLocked = false;
	}

	public void addDetailLevelEventListener( DetailLevelEventListener l ) {
		mDetailLevelEventListeners.add( l );
	}

	public void removeDetailLevelEventListener( DetailLevelEventListener l ) {
		mDetailLevelEventListeners.remove( l );
	}
	
	public void addDetailLevelSetupListener( DetailLevelSetupListener l ) {
		mDetailLevelSetupListeners.add( l );
	}

	public void removeDetailLevelSetupListener( DetailLevelSetupListener l ) {
		mDetailLevelSetupListeners.remove( l );
	}
	
	private void addDetailLevel( DetailLevel detailLevel ) {
		mDetailLevels.addDetailLevel( detailLevel );
		update( false );
		for ( DetailLevelSetupListener listener : mDetailLevelSetupListeners ) {
			listener.onDetailLevelAdded();
		}
	}
	
	public void addDetailLevel( float scale, Object data ) {
		DetailLevel detailLevel = new DetailLevel( this, scale, data );
		addDetailLevel( detailLevel );
	}
	
	public void addDetailLevel( float scale, Object data, int tileWidth, int tileHeight ) {
		DetailLevel detailLevel = new DetailLevel( this, scale, data, tileWidth, tileHeight );
		addDetailLevel( detailLevel );
	}
	
	public void resetDetailLevels(){
		mDetailLevels.clear();
		update( false );
	}

	public DetailLevel getCurrentDetailLevel() {
		return mCurrentDetailLevel;
	}
	
	public float getCurrentDetailLevelScale(){
		if( mCurrentDetailLevel != null ) {
			return mCurrentDetailLevel.getScale();
		}
		return 1;
	}
	
	public double getHistoricalScale(){
		return mHistoricalScale;
	}
	
	public void saveHistoricalScale(){
		mHistoricalScale = mScale;
	}

	public TileSetSelector getTileSetSelector() {
	    return this.mDetailLevels.getTileSetSelector();
	}

	/**
	 * Set the tile selection method, defaults to {@link TileSetSelectorMinimalUpScale}
	 * 
	 * @param selector
	 */
	public void setTileSetSelector(TileSetSelector selector) {
	    this.mDetailLevels.setTileSetSelector( selector );
	}

}
