package com.qozix.tileview.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

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
    measureChildren( widthMeasureSpec, heightMeasureSpec );
    int availableWidth = MeasureSpec.getSize( widthMeasureSpec );
    int availableHeight = MeasureSpec.getSize( heightMeasureSpec );
		setMeasuredDimension( availableWidth, availableHeight );
	}

  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {
    int availableWidth = r - l;
    int availableHeight = b - t;
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