package com.qozix.tileview.plugins;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import com.qozix.tileview.TileView;

// TODO: recycling
public class MarkerPlugin extends ViewGroup implements TileView.Plugin, TileView.Listener {

  private float mScale = 1;

  public MarkerPlugin(@NonNull Context context) {
    super(context);
  }

  @Override
  public void install(TileView tileView) {
    tileView.addListener(this);
    tileView.addView(this);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    measureChildren(widthMeasureSpec, heightMeasureSpec);
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      populateLayoutParams(child);
    }
    int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
    int availableHeight = MeasureSpec.getSize(heightMeasureSpec);
    setMeasuredDimension(availableWidth, availableHeight);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
        child.layout(layoutParams.mLeft, layoutParams.mTop, layoutParams.mRight, layoutParams.mBottom);
      }
    }
  }

  private LayoutParams populateLayoutParams(View child) {
    MarkerPlugin.LayoutParams layoutParams = (MarkerPlugin.LayoutParams) child.getLayoutParams();
    if (child.getVisibility() != View.GONE) {
      // actual sizes of children
      int actualWidth = child.getMeasuredWidth();
      int actualHeight = child.getMeasuredHeight();
      // calculate combined anchor offsets
      float widthOffset = actualWidth * layoutParams.relativeAnchorX + layoutParams.absoluteAnchorX;
      float heightOffset = actualHeight * layoutParams.relativeAnchorY + layoutParams.absoluteAnchorY;
      // get offset position
      int scaledX = (int) (layoutParams.x * mScale);
      int scaledY = (int) (layoutParams.y * mScale);
      // save computed values
      layoutParams.mLeft = (int) (scaledX + widthOffset);
      layoutParams.mTop = (int) (scaledY + heightOffset);
      layoutParams.mRight = layoutParams.mLeft + actualWidth;
      layoutParams.mBottom = layoutParams.mTop + actualHeight;
    }
    return layoutParams;
  }

  private void reposition() {
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        LayoutParams layoutParams = populateLayoutParams(child);
        child.setLeft(layoutParams.mLeft);
        child.setTop(layoutParams.mTop);
        child.setRight(layoutParams.mRight);
        child.setBottom(layoutParams.mBottom);
      }
    }
  }

  @Override
  public void onScaleChanged(float scale, float previous) {
    mScale = scale;
    reposition();
  }

  public void addMarker(View view, int left, int top, float relativeAnchorLeft, float relativeAnchorTop, float absoluteAnchorLeft, float absoluteAnchorTop) {
    LayoutParams layoutParams = new MarkerPlugin.LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT,
        left, top,
        relativeAnchorLeft, relativeAnchorTop,
        absoluteAnchorLeft, absoluteAnchorTop);
    addView(view, layoutParams);
  }

  public static class LayoutParams extends ViewGroup.LayoutParams {

    public int x;
    public int y;
    public float relativeAnchorX;
    public float relativeAnchorY;
    public float absoluteAnchorX;
    public float absoluteAnchorY;

    private int mTop;
    private int mLeft;
    private int mBottom;
    private int mRight;

    public LayoutParams(int width, int height, int left, int top, float relativeAnchorLeft, float relativeAnchorTop, float absoluteAnchorLeft, float absoluteAnchorTop) {
      super(width, height);
      x = left;
      y = top;
      relativeAnchorX = relativeAnchorLeft;
      relativeAnchorY = relativeAnchorTop;
      absoluteAnchorX = absoluteAnchorLeft;
      absoluteAnchorY = absoluteAnchorTop;
    }

  }

}
