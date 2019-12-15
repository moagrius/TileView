package com.moagrius.tileview.plugins;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

public class ScalingMarkerPlugin extends MarkerPlugin {
	private float mOriginalAtScale;

	public ScalingMarkerPlugin(@NonNull Context context) {
		this(context, 1f);
	}

	public ScalingMarkerPlugin(@NonNull Context context, float originalAtScale) {
		super(context);
		mOriginalAtScale = originalAtScale;
	}

	@Override
	protected LayoutParams populateLayoutParams(View child) {
		MarkerPlugin.LayoutParams layoutParams = (MarkerPlugin.LayoutParams) child.getLayoutParams();
		if (child.getVisibility() != View.GONE) {
			// actual sizes of children
			int measuredWidth = (int) (child.getMeasuredWidth() / mOriginalAtScale * mScale);
			int measuredHeight = (int) (child.getMeasuredHeight() / mOriginalAtScale * mScale);
			// calculate combined anchor offsets
			float widthOffset = measuredWidth * layoutParams.relativeAnchorX + layoutParams.absoluteAnchorX;
			float heightOffset = measuredHeight * layoutParams.relativeAnchorY + layoutParams.absoluteAnchorY;
			// get offset position
			int scaledX = (int) (layoutParams.x * mScale);
			int scaledY = (int) (layoutParams.y * mScale);
			// save computed values
			layoutParams.mLeft = (int) (scaledX + widthOffset);
			layoutParams.mTop = (int) (scaledY + heightOffset);
			layoutParams.mRight = layoutParams.mLeft + measuredWidth;
			layoutParams.mBottom = layoutParams.mTop + measuredHeight;
		}
		return layoutParams;
	}

	@Override
	public void onScaleChanged(float scale, float previous) {
		super.onScaleChanged(scale, previous);
		requestLayout();
	}
}
