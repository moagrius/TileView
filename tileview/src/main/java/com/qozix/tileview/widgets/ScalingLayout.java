package com.qozix.tileview.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import com.qozix.tileview.geom.FloatMathHelper;

public class ScalingLayout extends ViewGroup {

	private float mScale = 1;

  /**
   *
   * @param context
   */
  public ScalingLayout( Context context ) {
		super( context );
		setWillNotDraw( false );



	}

  /**
   *
   * @param scale
   */
	public void setScale( float scale ) {
		mScale = scale;
    invalidate();
	}

  /**
   *
   * @return
   */
	public float getScale() {
		return mScale;
	}

  /*
   * When scaling a canvas, as happens in onDraw, the clip area will be reduced at a small scale,
   * thus decreasing the drawable surface, but when scaled up, the canvas is still constrained
   * by the original width and height of the backing bitmap, which are not scaled.  Offset those
   * by dividing the measure and layout dimensions by the current scale.
   */

	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
    int availableWidth = FloatMathHelper.unscale( MeasureSpec.getSize( widthMeasureSpec ), mScale );
    int availableHeight = FloatMathHelper.unscale( MeasureSpec.getSize( heightMeasureSpec ), mScale );
    // the container's children should be the size provided by setSize
    // don't use measureChildren because that grabs the child's LayoutParams
    int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec( availableWidth, MeasureSpec.EXACTLY );
    int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec( availableHeight, MeasureSpec.EXACTLY );
    for( int i = 0; i < getChildCount(); i++){
      View child = getChildAt( i );
      child.measure( childWidthMeasureSpec, childHeightMeasureSpec );
    }
		setMeasuredDimension( availableWidth, availableHeight );
	}

  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {
    int availableWidth = FloatMathHelper.unscale( r - l, mScale );
    int availableHeight = FloatMathHelper.unscale( b - t, mScale );
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