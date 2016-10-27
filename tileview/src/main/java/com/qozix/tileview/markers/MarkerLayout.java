package com.qozix.tileview.markers;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import com.qozix.tileview.geom.FloatMathHelper;

public class MarkerLayout extends ViewGroup {

  private float mScale = 1;

  private float mAnchorX;
  private float mAnchorY;

  private MarkerTapListener mMarkerTapListener;

  public MarkerLayout( Context context ) {
    super( context );
    setClipChildren( false );
  }

  /**
   * Sets the anchor values used by this ViewGroup if it's children do not
   * have anchor values supplied directly (via individual LayoutParams).
   *
   * @param aX x-axis anchor value (offset computed by multiplying this value by the child's width).
   * @param aY y-axis anchor value (offset computed by multiplying this value by the child's height).
   */
  public void setAnchors( float aX, float aY ) {
    mAnchorX = aX;
    mAnchorY = aY;
    requestLayout();
  }

  /**
   * Sets the scale (0-1) of the MarkerLayout.
   *
   * @param scale The new value of the MarkerLayout scale.
   */
  public void setScale( float scale ) {
    mScale = scale;
    requestLayout();
  }

  /**
   * Retrieves the current scale of the MarkerLayout.
   *
   * @return The current scale of the MarkerLayout.
   */
  public float getScale() {
    return mScale;
  }

  public View addMarker( View view, int x, int y, Float aX, Float aY ) {
    ViewGroup.LayoutParams defaultLayoutParams = view.getLayoutParams();
    LayoutParams markerLayoutParams = (defaultLayoutParams != null)
      ? generateLayoutParams(defaultLayoutParams)
      : generateDefaultLayoutParams();
    markerLayoutParams.x = x;
    markerLayoutParams.y = y;
    markerLayoutParams.anchorX = aX;
    markerLayoutParams.anchorY = aY;
    return addMarker( view, markerLayoutParams );
  }

  public View addMarker( View view, LayoutParams params ) {
    addView( view, params );
    return view;
  }

  public void moveMarker( View view, int x, int y ) {
    LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
    layoutParams.x = x;
    layoutParams.y = y;
    moveMarker( view, layoutParams );
  }

  public void moveMarker( View view, LayoutParams params ) {
    if( indexOfChild( view ) > -1 ) {
      view.setLayoutParams( params );
      requestLayout();
    }
  }

  public void removeMarker( View view ) {
    removeView( view );
  }

  public void setMarkerTapListener( MarkerTapListener markerTapListener ) {
    mMarkerTapListener = markerTapListener;
  }

  private View getViewFromTap( int x, int y ) {
    for( int i = getChildCount() - 1; i >= 0; i-- ) {
      View child = getChildAt( i );
      LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
      Rect hitRect = layoutParams.getHitRect();
      if( hitRect.contains( x, y ) ) {
        return child;
      }
    }
    return null;
  }

  public void processHit( int x, int y ) {
    if( mMarkerTapListener != null ) {
      View view = getViewFromTap( x, y );
      if( view != null ) {
        mMarkerTapListener.onMarkerTap( view, x, y );
      }
    }
  }

  @Override
  protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
    measureChildren( widthMeasureSpec, heightMeasureSpec );
    for( int i = 0; i < getChildCount(); i++ ) {
      View child = getChildAt( i );
      if( child.getVisibility() != GONE ) {
        MarkerLayout.LayoutParams layoutParams = (MarkerLayout.LayoutParams) child.getLayoutParams();
        // get anchor offsets
        float widthMultiplier = (layoutParams.anchorX == null) ? mAnchorX : layoutParams.anchorX;
        float heightMultiplier = (layoutParams.anchorY == null) ? mAnchorY : layoutParams.anchorY;
        // actual sizes of children
        int actualWidth = child.getMeasuredWidth();
        int actualHeight = child.getMeasuredHeight();
        // offset dimensions by anchor values
        float widthOffset = actualWidth * widthMultiplier;
        float heightOffset = actualHeight * heightMultiplier;
        // get offset position
        int scaledX = FloatMathHelper.scale( layoutParams.x, mScale );
        int scaledY = FloatMathHelper.scale( layoutParams.y, mScale );
        // save computed values
        layoutParams.mLeft = (int) (scaledX + widthOffset);
        layoutParams.mTop = (int) (scaledY + heightOffset);
        layoutParams.mRight = layoutParams.mLeft + actualWidth;
        layoutParams.mBottom = layoutParams.mTop + actualHeight;
      }
    }
    int availableWidth = MeasureSpec.getSize( widthMeasureSpec );
    int availableHeight = MeasureSpec.getSize( heightMeasureSpec );
    setMeasuredDimension( availableWidth, availableHeight );
  }

  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {
    for( int i = 0; i < getChildCount(); i++ ) {
      View child = getChildAt( i );
      if( child.getVisibility() != GONE ) {
        LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
        child.layout( layoutParams.mLeft, layoutParams.mTop, layoutParams.mRight, layoutParams.mBottom );
      }
    }
  }

  @Override
  protected MarkerLayout.LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0 );
  }

  @Override
  protected boolean checkLayoutParams( ViewGroup.LayoutParams layoutParams ) {
    return layoutParams instanceof LayoutParams;
  }

  @Override
  protected MarkerLayout.LayoutParams generateLayoutParams( ViewGroup.LayoutParams layoutParams ) {
    return new LayoutParams( layoutParams );
  }

  /**
   * Per-child layout information associated with AnchorLayout.
   */
  public static class LayoutParams extends ViewGroup.LayoutParams {

    /**
     * The absolute left position of the child in pixels.
     */
    public int x = 0;

    /**
     * The absolute right position of the child in pixels.
     */
    public int y = 0;

    /**
     * Float value to determine the child's horizontal offset.
     * This float is multiplied by the child's width.
     * If null, the containing AnchorLayout's anchor values will be used.
     */
    public Float anchorX = null;

    /**
     * Float value to determine the child's vertical offset.
     * This float is multiplied by the child's height.
     * If null, the containing AnchorLayout's anchor values will be used.
     */
    public Float anchorY = null;

    private int mTop;
    private int mLeft;
    private int mBottom;
    private int mRight;

    private Rect mHitRect;

    private Rect getHitRect() {
      if( mHitRect == null ) {
        mHitRect = new Rect();
      }
      mHitRect.left = mLeft;
      mHitRect.top = mTop;
      mHitRect.right = mRight;
      mHitRect.bottom = mBottom;
      return mHitRect;
    }

    /**
     * Copy constructor.
     *
     * @param source LayoutParams instance to copy properties from.
     */
    public LayoutParams( ViewGroup.LayoutParams source ) {
      super( source );
    }

    /**
     * Creates a new set of layout parameters with the specified values.
     *
     * @param width  Information about how wide the view wants to be.  This should generally be WRAP_CONTENT or a fixed value.
     * @param height Information about how tall the view wants to be.  This should generally be WRAP_CONTENT or a fixed value.
     */
    public LayoutParams( int width, int height ) {
      super( width, height );
    }

    /**
     * Creates a new set of layout parameters with the specified values.
     *
     * @param width  Information about how wide the view wants to be.  This should generally be WRAP_CONTENT or a fixed value.
     * @param height Information about how tall the view wants to be.  This should generally be WRAP_CONTENT or a fixed value.
     * @param left   Sets the absolute x value of the view's position in pixels.
     * @param top    Sets the absolute y value of the view's position in pixels.
     */
    public LayoutParams( int width, int height, int left, int top ) {
      super( width, height );
      x = left;
      y = top;
    }

    /**
     * Creates a new set of layout parameters with the specified values.
     *
     * @param width      Information about how wide the view wants to be.  This should generally be WRAP_CONTENT or a fixed value.
     * @param height     Information about how tall the view wants to be.  This should generally be WRAP_CONTENT or a fixed value.
     * @param left       Sets the absolute x value of the view's position in pixels.
     * @param top        Sets the absolute y value of the view's position in pixels.
     * @param anchorLeft Sets the relative horizontal offset of the view (multiplied by the view's width).
     * @param anchorTop  Sets the relative vertical offset of the view (multiplied by the view's height).
     */
    public LayoutParams( int width, int height, int left, int top, Float anchorLeft, Float anchorTop ) {
      super( width, height );
      x = left;
      y = top;
      anchorX = anchorLeft;
      anchorY = anchorTop;
    }

  }

  public interface MarkerTapListener {
    void onMarkerTap( View view, int x, int y );
  }
}
