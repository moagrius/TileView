package com.qozix.tileview.layouts;

import android.content.Context;
import android.graphics.Canvas;

public class ScalingLayout extends FixedLayout {

	private float scale = 1;

	public ScalingLayout( Context context ) {
		super( context );
		setWillNotDraw( false );
	}

	public void setScale( float factor ) {
		scale = factor;
		postInvalidate();
	}

	public float getScale() {
		return scale;
	}

	@Override
	public void onDraw( Canvas canvas ) {
		canvas.scale(  scale, scale );
		super.onDraw( canvas );
	}

}