package com.qozix.tileview.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

public class ScalingLayout extends ViewGroup {

	private float mScale = 1;

  private Rect mClipRect = new Rect();

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
		int width = MeasureSpec.getSize( widthMeasureSpec );
		int height = MeasureSpec.getSize( heightMeasureSpec );
		setMeasuredDimension( width, height );
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

  private void scaleCanvasBounds( Canvas canvas ){
    canvas.getClipBounds( mClipRect );
    mClipRect.top *= mScale;
    mClipRect.left *= mScale;
    mClipRect.bottom *= mScale;
    mClipRect.right *= mScale;
    canvas.clipRect( mClipRect );
  }

  @Override
  public void onDraw( Canvas canvas ) {
    canvas.scale( mScale, mScale );
    scaleCanvasBounds( canvas );
    super.onDraw( canvas );
  }

}