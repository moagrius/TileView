package com.qozix.tileview.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
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
		invalidate();
	}

	public float getScale() {
		return mScale;
	}

	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
    //measureChildren( widthMeasureSpec, heightMeasureSpec );

    int availableWidth = MeasureSpec.getSize( widthMeasureSpec );
    int availableHeight = MeasureSpec.getSize( heightMeasureSpec );
    int childWidthSpec = MeasureSpec.makeMeasureSpec( (int) (availableWidth / mScale), MeasureSpec.AT_MOST );
    int childHeightSpec = MeasureSpec.makeMeasureSpec( (int) (availableHeight / mScale ), MeasureSpec.AT_MOST );
    measureChildren( childWidthSpec, childHeightSpec );
    Log.d( "TileView", "ScalingLayout.onMeasure: " + availableWidth + ", " + availableHeight );
		setMeasuredDimension( availableWidth, availableHeight );
	}


  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {
    int availableWidth = (int) (( r - l ) / mScale );
    int availableHeight = (int) (( b - t ) / mScale );
    Log.d( "TileView", "ScalingLayout.onLayout: " + availableWidth + ", " + availableHeight );
    for( int i = 0; i < getChildCount(); i++ ) {
      View child = getChildAt( i );
      if( child.getVisibility() != GONE ) {
        child.layout( 0, 0, availableWidth, availableHeight );
      }
    }
  }

  @Override
  public void onDraw( Canvas canvas ) {
    canvas.scale( mScale, mScale );
    super.onDraw( canvas );
  }

}