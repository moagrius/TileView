package com.qozix.tileview.detail;

import android.graphics.Rect;

import com.qozix.tileview.tiles.Tile;

import java.util.LinkedList;

public class DetailLevel implements Comparable<DetailLevel> {

	private static final int DEFAULT_TILE_SIZE = 256;

	private double mScale;
	
	private int mTileWidth = DEFAULT_TILE_SIZE;
	private int mTileHeight = DEFAULT_TILE_SIZE;

	private Object mData;

	private DetailManager mDetailManager;
	private Rect mViewport = new Rect();

	public DetailLevel( DetailManager detailManager, float scale, Object data, int tileWidth, int tileHeight ) {
		mDetailManager = detailManager;
		mScale = scale;
		mData = data;
		mTileWidth = tileWidth;
		mTileHeight = tileHeight;
	}
	
	public DetailLevel( DetailManager detailManager, float scale, Object data ) {
		this( detailManager, scale, data, DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE );
	}

  public boolean isTileInViewport( Tile tile ) {
    return mViewport.contains( tile.getRect() );
  }

	public LinkedList<Tile> getIntersections() {
		
		double relativeScale = getRelativeScale();
		
		int drawableWidth = (int) ( mDetailManager.getWidth() * getScale() * relativeScale );
		int drawableHeight = (int) ( mDetailManager.getHeight() * getScale() * relativeScale );
		double offsetWidth = ( mTileWidth * relativeScale );
		double offsetHeight = ( mTileHeight * relativeScale );

		mViewport.set( mDetailManager.getComputedViewport() );
		
		mViewport.top = Math.max( mViewport.top, 0 );
		mViewport.left = Math.max( mViewport.left, 0 );
		mViewport.right = Math.min( mViewport.right, drawableWidth );
		mViewport.bottom = Math.min( mViewport.bottom, drawableHeight );

		int startingRow = (int) Math.floor( mViewport.top / offsetHeight );
		int endingRow = (int) Math.ceil( mViewport.bottom / offsetHeight );
		int startingColumn = (int) Math.floor( mViewport.left / offsetWidth );
		int endingColumn = (int) Math.ceil( mViewport.right / offsetWidth );

		LinkedList<Tile> intersections = new LinkedList<Tile>();

		for ( int iterationRow = startingRow; iterationRow < endingRow; iterationRow++ ) {
			for ( int iterationColumn = startingColumn; iterationColumn < endingColumn; iterationColumn++ ) {
				Tile tile = new Tile( iterationColumn, iterationRow, mTileWidth, mTileHeight, mData );
				intersections.add( tile );
			}
		}
		
		return intersections;
		
	}

	public double getScale(){
		return mScale;
	}
	
	public double getRelativeScale(){
		return mDetailManager.getScale() / mScale;
	}
	
	public int getTileWidth() {
		return mTileWidth;
	}

	public int getTileHeight() {
		return mTileHeight;
	}

	public Object getData(){
		return mData;
	}

	@Override
	public int compareTo( DetailLevel o ) {
		return (int) Math.signum( getScale() - o.getScale() );
	}

	@Override
	public boolean equals( Object o ) {
		if ( o instanceof DetailLevel) {
			DetailLevel zl = (DetailLevel) o;
			return ( zl.getScale() == getScale() );
		}
		return false;
	}

	@Override
	public int hashCode() {
		long bits = ( Double.doubleToLongBits( getScale() ) * 43 );
		return ( ( (int) bits ) ^ ( (int) ( bits >> 32 ) ) );
	}

	
}