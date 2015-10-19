package com.qozix.tileview.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

public class ScalingLayout extends ViewGroup {

	private float mScale = 1;

	public ScalingLayout( Context context ) {
		super( context );
		setWillNotDraw( false );
	}

	public void setScale( float factor ) {
		mScale = factor;
		postInvalidate();
	}

	public float getScale() {
		return mScale;
	}

	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
		measureChildren( widthMeasureSpec, heightMeasureSpec );
		int width = MeasureSpec.getSize( widthMeasureSpec );
		int height = MeasureSpec.getSize( heightMeasureSpec );
		width = Math.max( width, getSuggestedMinimumWidth() );
		height = Math.max( height, getSuggestedMinimumHeight() );
		width = resolveSize( width, widthMeasureSpec );
		height = resolveSize( height, heightMeasureSpec );
		setMeasuredDimension( width, height );
	}

	@Override
	protected void onLayout( boolean changed, int l, int t, int r, int b ) {
		for( int i = 0; i < getChildCount(); i++ ) {
			View child = getChildAt( i );
			if( child.getVisibility() != GONE ) {
				child.layout( 0, 0, r - l, b - t );  // TODO: might need to setSize
        // TODO: does the r-l,b-t thing work?
			}
		}
	}

	@Override
	public void onDraw( Canvas canvas ) {
		canvas.scale( mScale, mScale );
		super.onDraw( canvas );
	}

}