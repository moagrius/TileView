package com.qozix.tileview.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import com.qozix.tileview.geom.FloatMathHelper;
import com.qozix.tileview.view.IScalingCanvasView;

public class ScalingLayout extends ViewGroup implements IScalingCanvasView {

	private float mScale = 1;

  public ScalingLayout( Context context ) {
		super( context );
		setWillNotDraw( false );
	}

	public void setScale( float scale ) {
		mScale = scale;
    invalidate();
	}

	public float getScale() {
		return mScale;
	}

	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
    //measureChildren( widthMeasureSpec, heightMeasureSpec );
    int availableWidth = FloatMathHelper.unscale( MeasureSpec.getSize( widthMeasureSpec ), mScale );
    int availableHeight = FloatMathHelper.unscale( MeasureSpec.getSize( heightMeasureSpec ), mScale );

    measureChildren(
      MeasureSpec.makeMeasureSpec( availableWidth, MeasureSpec.EXACTLY ),
      MeasureSpec.makeMeasureSpec( availableHeight, MeasureSpec.EXACTLY ) );

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