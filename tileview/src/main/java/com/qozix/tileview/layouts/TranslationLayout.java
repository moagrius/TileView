package com.qozix.tileview.layouts;

import android.content.Context;
import android.view.View;

/**
 * The TranslationLayout extends {@link AnchorLayout}, but additionally supports
 * a mScale value.  The views of this layout will not be scaled along width or height,
 * but their positions will be multiplied by the TranslationLayout's mScale value.
 * This allows the contained views to maintain their visual appearance and distance
 * relative to each other, while the total area of the group can be managed by the
 * mScale value.
 * <p/>
 * This is useful for positioning groups of markers, tooltips, or indicator views
 * without scaling, while the reference element(s) are scaled.
 */

public class TranslationLayout extends AnchorLayout {

  protected float mScale = 1;

  public TranslationLayout( Context context ) {
    super( context );
  }

  /**
   * Sets the scale (0-1) of the TranslationLayout
   *
   * @param scale The new value of the ZoomPanLayout mScale
   */
  public void setScale( float scale ) {
    mScale = scale;
    requestLayout();
  }

  /**
   * Retrieves the current mScale of the ZoomPanLayout
   *
   * @return (double) the current mScale of the ZoomPanLayout
   */
  public float getScale() {
    return mScale;
  }

  @Override
  protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {

    measureChildren( widthMeasureSpec, heightMeasureSpec );

    int width = 0;
    int height = 0;

    int count = getChildCount();
    for( int i = 0; i < count; i++ ) {
      View child = getChildAt( i );
      if( child.getVisibility() != GONE ) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        // get anchor offsets
        float aX = (lp.anchorX == null) ? anchorX : lp.anchorX;
        float aY = (lp.anchorY == null) ? anchorY : lp.anchorY;
        // offset dimensions by anchor values
        int computedWidth = (int) (child.getMeasuredWidth() * aX);
        int computedHeight = (int) (child.getMeasuredHeight() * aY);
        // get offset position
        int scaledX = (int) (0.5 + (lp.x * mScale));
        int scaledY = (int) (0.5 + (lp.y * mScale));
        // add computed dimensions to actual position
        int right = scaledX + computedWidth;
        int bottom = scaledY + computedHeight;
        // if it's larger, use that
        width = Math.max( width, right );
        height = Math.max( height, bottom );
      }
    }

    height = Math.max( height, getSuggestedMinimumHeight() );
    width = Math.max( width, getSuggestedMinimumWidth() );
    width = resolveSize( width, widthMeasureSpec );
    height = resolveSize( height, heightMeasureSpec );
    setMeasuredDimension( width, height );

  }

  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {
    // TODO: can any of this be moved to onMeasure
    int count = getChildCount();
    for( int i = 0; i < count; i++ ) {
      View child = getChildAt( i );
      if( child.getVisibility() != GONE ) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        // get sizes
        int w = child.getMeasuredWidth();
        int h = child.getMeasuredHeight();
        // get offset position
        int scaledX = (int) (0.5 + (lp.x * mScale));
        int scaledY = (int) (0.5 + (lp.y * mScale));
        // user child's layout params anchor position if set, otherwise default to anchor position of layout
        float aX = (lp.anchorX == null) ? anchorX : lp.anchorX;
        float aY = (lp.anchorY == null) ? anchorY : lp.anchorY;
        // apply anchor offset to position
        int x = scaledX + (int) (w * aX);
        int y = scaledY + (int) (h * aY);
        // set it
        child.layout( x, y, x + w, y + h );
      }
    }
  }

}
