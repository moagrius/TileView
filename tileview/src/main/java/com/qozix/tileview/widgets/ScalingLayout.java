package com.qozix.tileview.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class ScalingLayout extends ViewGroup implements IScalingCanvas {

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

    measureChildren( widthMeasureSpec, heightMeasureSpec );

    int availableWidth = MeasureSpec.getSize( widthMeasureSpec );
    int availableHeight = MeasureSpec.getSize( heightMeasureSpec );

    Log.d( "TileView", "ScalingLayout.onMeasure: " + availableWidth + ", " + availableHeight );

    /*
    ZoomPanLayout.LayoutParams layoutParams = (ZoomPanLayout.LayoutParams) getLayoutParams();

    // Views that scale must think they are as much bigger as they are than they would scale to
    int availableWidth = (int) (layoutParams.baseWidth / mScale);
    int availableHeight = (int) (layoutParams.baseHeight / mScale);

    measureChildren(
      MeasureSpec.makeMeasureSpec( availableWidth, MeasureSpec.EXACTLY ),
      MeasureSpec.makeMeasureSpec( availableHeight, MeasureSpec.EXACTLY )
    );


    Log.d( "TileView", "ScalingLayout.onMeasure: "
      + "(" + MeasureSpec.getSize( widthMeasureSpec ) + ", " + MeasureSpec.getSize( heightMeasureSpec ) + ") vs "
      + "(" + layoutParams.scaledWidth + ", " + layoutParams.scaledHeight + ") vs "
      + "(" + layoutParams.baseWidth + ", " + layoutParams.baseHeight + ") vs "
      + "(" + availableWidth + ", " + availableHeight + ")");
    */

		setMeasuredDimension( availableWidth, availableHeight );
	}


  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {

    int availableWidth = r - l;
    int availableHeight = b - t;
    /*
    ZoomPanLayout.LayoutParams layoutParams = (ZoomPanLayout.LayoutParams) getLayoutParams();
    int availableWidth = layoutParams.scaledWidth;
    int availableHeight = layoutParams.scaledHeight;
    */
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
    Log.d( "TileView", "ScalingLayout.onMeasure (before scale): " + canvas.getWidth() + ", " + canvas.getHeight() );
    canvas.scale( mScale, mScale );
    Log.d( "TileView", "ScalingLayout.onMeasure (after scale): " + canvas.getWidth() + ", " + canvas.getHeight() );
    super.onDraw( canvas );
  }

}