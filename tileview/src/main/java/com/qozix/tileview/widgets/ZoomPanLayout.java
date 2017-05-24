package com.qozix.tileview.widgets;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import com.qozix.tileview.geom.FloatMathHelper;
import com.qozix.tileview.view.TouchUpGestureDetector;

import java.lang.ref.WeakReference;
import java.util.HashSet;

/**
 * ZoomPanLayout extends ViewGroup to provide support for scrolling and zooming.
 * Fling, drag, pinch and double-tap events are supported natively.
 *
 * Children of ZoomPanLayout are laid out to the sizes provided by setSize,
 * and will always be positioned at 0,0.
 */

public class ZoomPanLayout extends ViewGroup implements
  GestureDetector.OnGestureListener,
  GestureDetector.OnDoubleTapListener,
  ScaleGestureDetector.OnScaleGestureListener,
  TouchUpGestureDetector.OnTouchUpListener {

  private static final int DEFAULT_ZOOM_PAN_ANIMATION_DURATION = 400;

  private int mBaseWidth;
  private int mBaseHeight;
  private int mScaledWidth;
  private int mScaledHeight;

  private float mScale = 1;

  private float mMinScale = 0;
  private float mMaxScale = 1;

  private int mOffsetX;
  private int mOffsetY;

  private float mEffectiveMinScale = 0;
  private float mMinimumScaleX;
  private float mMinimumScaleY;
  private boolean mShouldLoopScale = true;

  private boolean mIsFlinging;
  private boolean mIsDragging;
  private boolean mIsScaling;
  private boolean mIsSliding;

  private int mAnimationDuration = DEFAULT_ZOOM_PAN_ANIMATION_DURATION;

  private HashSet<ZoomPanListener> mZoomPanListeners = new HashSet<ZoomPanListener>();

  private Scroller mScroller;
  private ZoomPanAnimator mZoomPanAnimator;

  private ScaleGestureDetector mScaleGestureDetector;
  private GestureDetector mGestureDetector;
  private TouchUpGestureDetector mTouchUpGestureDetector;
  private MinimumScaleMode mMinimumScaleMode = MinimumScaleMode.FILL;

  /**
   * Constructor to use when creating a ZoomPanLayout from code.
   *
   * @param context The Context the ZoomPanLayout is running in, through which it can access the current theme, resources, etc.
   */
  public ZoomPanLayout( Context context ) {
    this( context, null );
  }

  public ZoomPanLayout( Context context, AttributeSet attrs ) {
    this( context, attrs, 0 );
  }

  public ZoomPanLayout( Context context, AttributeSet attrs, int defStyleAttr ) {
    super( context, attrs, defStyleAttr );
    setWillNotDraw( false );
    setClipChildren( false );
    mGestureDetector = new GestureDetector( context, this );
    mScaleGestureDetector = new ScaleGestureDetector( context, this );
    mTouchUpGestureDetector = new TouchUpGestureDetector( this );
  }

  @Override
  protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
    // the container's children should be the size provided by setSize
    // don't use measureChildren because that grabs the child's LayoutParams
    int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec( mScaledWidth, MeasureSpec.EXACTLY );
    int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec( mScaledHeight, MeasureSpec.EXACTLY );
    for( int i = 0; i < getChildCount(); i++){
      View child = getChildAt( i );
      child.measure( childWidthMeasureSpec, childHeightMeasureSpec );
    }
    // but the layout itself should report normal (on screen) dimensions
    int width = MeasureSpec.getSize( widthMeasureSpec );
    int height = MeasureSpec.getSize( heightMeasureSpec );
    width = resolveSize( width, widthMeasureSpec );
    height = resolveSize( height, heightMeasureSpec );
    setMeasuredDimension( width, height );
  }

  /*
  ZoomPanChildren will always be laid out with the scaled dimenions - what is visible during
  scroll operations.  Thus, a RelativeLayout added as a child that had views within it using
  rules like ALIGN_PARENT_RIGHT would function as expected; similarly, an ImageView would be
  stretched between the visible edges.
  If children further operate on scale values, that should be accounted for
  in the child's logic (see ScalingLayout).
   */
  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {
    final int width = getWidth();
    final int height = getHeight();

    mOffsetX = mScaledWidth >= width ? 0 : width / 2 - mScaledWidth / 2;
    mOffsetY = mScaledHeight >= height ? 0 : height / 2 - mScaledHeight / 2;

    for( int i = 0; i < getChildCount(); i++ ) {
      View child = getChildAt( i );
      if( child.getVisibility() != GONE ) {
        child.layout( mOffsetX, mOffsetY, mScaledWidth + mOffsetX, mScaledHeight + mOffsetY );
      }
    }
    calculateMinimumScaleToFit();
    constrainScrollToLimits();
  }

  /**
   * Determines whether the ZoomPanLayout should limit it's minimum scale to no less than what
   * would be required to fill it's container.
   *
   * @param shouldScaleToFit True to limit minimum scale, false to allow arbitrary minimum scale.
   */
  public void setShouldScaleToFit( boolean shouldScaleToFit ) {
    setMinimumScaleMode(shouldScaleToFit ? MinimumScaleMode.FILL : MinimumScaleMode.NONE);
  }

  /**
   * Sets the minimum scale mode
   *
   * @param minimumScaleMode The minimum scale mode
   */
  public void setMinimumScaleMode( MinimumScaleMode minimumScaleMode ) {
    mMinimumScaleMode = minimumScaleMode;
    calculateMinimumScaleToFit();
  }

  /**
   * Determines whether the ZoomPanLayout should go back to minimum scale after a double-tap at
   * maximum scale.
   *
   * @param shouldLoopScale True to allow going back to minimum scale, false otherwise.
   */
  public void setShouldLoopScale( boolean shouldLoopScale ) {
    mShouldLoopScale = shouldLoopScale;
  }

  /**
   * Set minimum and maximum mScale values for this ZoomPanLayout.
   * Note that if minimumScaleMode is set to {@link MinimumScaleMode#FIT} or {@link MinimumScaleMode#FILL}, the minimum value set here will be ignored
   * Default values are 0 and 1.
   *
   * @param min Minimum scale the ZoomPanLayout should accept.
   * @param max Maximum scale the ZoomPanLayout should accept.
   */
  public void setScaleLimits( float min, float max ) {
    mMinScale = min;
    mMaxScale = max;
    setScale( mScale );
  }

  /**
   * Sets the size (width and height) of the ZoomPanLayout
   * as it should be rendered at a scale of 1f (100%).
   *
   * @param width  Width of the underlying image, not the view or viewport.
   * @param height Height of the underlying image, not the view or viewport.
   */
  public void setSize( int width, int height ) {
    mBaseWidth = width;
    mBaseHeight = height;
    updateScaledDimensions();
    calculateMinimumScaleToFit();
    constrainScrollToLimits();
    requestLayout();
  }

  /**
   * Returns the base (not scaled) width of the underlying composite image.
   *
   * @return The base (not scaled) width of the underlying composite image.
   */
  public int getBaseWidth() {
    return mBaseWidth;
  }

  /**
   * Returns the base (not scaled) height of the underlying composite image.
   *
   * @return The base (not scaled) height of the underlying composite image.
   */
  public int getBaseHeight() {
    return mBaseHeight;
  }

  /**
   * Returns the scaled width of the underlying composite image.
   *
   * @return The scaled width of the underlying composite image.
   */
  public int getScaledWidth() {
    return mScaledWidth;
  }

  /**
   * Returns the scaled height of the underlying composite image.
   *
   * @return The scaled height of the underlying composite image.
   */
  public int getScaledHeight() {
    return mScaledHeight;
  }

  /**
   * Sets the scale (0-1) of the ZoomPanLayout.
   *
   * @param scale The new value of the ZoomPanLayout scale.
   */
  public void setScale( float scale ) {
    scale = getConstrainedDestinationScale( scale );
    if( mScale != scale ) {
      float previous = mScale;
      mScale = scale;
      updateScaledDimensions();
      constrainScrollToLimits();
      onScaleChanged( scale, previous );
      invalidate();
    }
  }

  /**
   * Retrieves the current scale of the ZoomPanLayout.
   *
   * @return The current scale of the ZoomPanLayout.
   */
  public float getScale() {
    return mScale;
  }

  /**
   * Returns the horizontal distance children are offset if the content is scaled smaller than width.
   *
   * @return
   */
  public int getOffsetX() {
    return mOffsetX;
  }

  /**
   * Return the vertical distance children are offset if the content is scaled smaller than height.
   *
   * @return
   */
  public int getOffsetY() {
    return mOffsetY;
  }

  /**
   * Returns whether the ZoomPanLayout is currently being flung.
   *
   * @return true if the ZoomPanLayout is currently flinging, false otherwise.
   */
  public boolean isFlinging() {
    return mIsFlinging;
  }

  /**
   * Returns whether the ZoomPanLayout is currently being dragged.
   *
   * @return true if the ZoomPanLayout is currently dragging, false otherwise.
   */
  public boolean isDragging() {
    return mIsDragging;
  }

  /**
   * Returns whether the ZoomPanLayout is currently operating a scroll tween.
   *
   * @return True if the ZoomPanLayout is currently scrolling, false otherwise.
   */
  public boolean isSliding() {
    return mIsSliding;
  }

  /**
   * Returns whether the ZoomPanLayout is currently operating a scale tween.
   *
   * @return True if the ZoomPanLayout is currently scaling, false otherwise.
   */
  public boolean isScaling() {
    return mIsScaling;
  }

  /**
   * Returns the Scroller instance used to manage dragging and flinging.
   *
   * @return The Scroller instance use to manage dragging and flinging.
   */
  public Scroller getScroller() {
    // Instantiate default scroller if none is available
    if( mScroller == null ){
      mScroller = new Scroller( getContext() );
    }
    return mScroller;
  }

  /**
   * Returns the duration zoom and pan animations will use.
   *
   * @return The duration zoom and pan animations will use.
   */
  public int getAnimationDuration() {
    return mAnimationDuration;
  }

  /**
   * Set the duration zoom and pan animation will use.
   *
   * @param animationDuration The duration animations will use.
   */
  public void setAnimationDuration( int animationDuration ) {
    mAnimationDuration = animationDuration;
    if( mZoomPanAnimator != null ) {
      mZoomPanAnimator.setDuration( mAnimationDuration );
    }
  }

  /**
   * Adds a ZoomPanListener to the ZoomPanLayout, which will receive notification of actions
   * relating to zoom and pan events.
   *
   * @param zoomPanListener ZoomPanListener implementation to add.
   * @return True when the listener set did not already contain the Listener, false otherwise.
   */
  public boolean addZoomPanListener( ZoomPanListener zoomPanListener ) {
    return mZoomPanListeners.add( zoomPanListener );
  }

  /**
   * Removes a ZoomPanListener from the ZoomPanLayout
   *
   * @param listener ZoomPanListener to remove.
   * @return True if the Listener was removed, false otherwise.
   */
  public boolean removeZoomPanListener( ZoomPanListener listener ) {
    return mZoomPanListeners.remove( listener );
  }

  /**
   * Scrolls and centers the ZoomPanLayout to the x and y values provided.
   *
   * @param x Horizontal destination point.
   * @param y Vertical destination point.
   */
  public void scrollToAndCenter( int x, int y ) {
    scrollTo( x - getHalfWidth(), y - getHalfHeight() );
  }

  /**
   * Set the scale of the ZoomPanLayout while maintaining the current center point.
   *
   * @param scale The new value of the ZoomPanLayout scale.
   */
  public void setScaleFromCenter( float scale ) {
    setScaleFromPosition( getHalfWidth(), getHalfHeight(), scale );
  }

  /**
   * Scrolls the ZoomPanLayout to the x and y values provided using scrolling animation.
   *
   * @param x Horizontal destination point.
   * @param y Vertical destination point.
   */
  public void slideTo( int x, int y ) {
    getAnimator().animatePan( x, y );
  }

  /**
   * Scrolls and centers the ZoomPanLayout to the x and y values provided using scrolling animation.
   *
   * @param x Horizontal destination point.
   * @param y Vertical destination point.
   */
  public void slideToAndCenter( int x, int y ) {
    slideTo( x - getHalfWidth(), y - getHalfHeight() );
  }

  /**
   * Animates the ZoomPanLayout to the scale provided, and centers the viewport to the position
   * supplied.
   *
   * @param x Horizontal destination point.
   * @param y Vertical destination point.
   * @param scale The final scale value the ZoomPanLayout should animate to.
   */
  public void slideToAndCenterWithScale( int x, int y, float scale ) {
    getAnimator().animateZoomPan( x - getHalfWidth(), y - getHalfHeight(), scale );
  }

  /**
   * Scales the ZoomPanLayout with animated progress, without maintaining scroll position.
   *
   * @param destination The final scale value the ZoomPanLayout should animate to.
   */
  public void smoothScaleTo( float destination ) {
    getAnimator().animateZoom( destination );
  }

  /**
   * Animates the ZoomPanLayout to the scale provided, while maintaining position determined by
   * the focal point provided.
   *
   * @param focusX The horizontal focal point to maintain, relative to the screen (as supplied by MotionEvent.getX).
   * @param focusY The vertical focal point to maintain, relative to the screen (as supplied by MotionEvent.getY).
   * @param scale The final scale value the ZoomPanLayout should animate to.
   */
  public void smoothScaleFromFocalPoint( int focusX, int focusY, float scale ) {
    scale = getConstrainedDestinationScale( scale );
    if( scale == mScale ) {
      return;
    }
    int x = getOffsetScrollXFromScale( focusX, scale, mScale );
    int y = getOffsetScrollYFromScale( focusY, scale, mScale );
    getAnimator().animateZoomPan( x, y, scale );
  }

  /**
   * Animate the scale of the ZoomPanLayout while maintaining the current center point.
   *
   * @param scale The final scale value the ZoomPanLayout should animate to.
   */
  public void smoothScaleFromCenter( float scale ) {
    smoothScaleFromFocalPoint( getHalfWidth(), getHalfHeight(), scale );
  }

  /**
   * Provide this method to be overriden by subclasses, e.g., onScrollChanged.
   */
  public void onScaleChanged( float currentScale, float previousScale ) {
    // noop
  }

  private float getConstrainedDestinationScale( float scale ) {
    scale = Math.max( scale, mEffectiveMinScale );
    scale = Math.min( scale, mMaxScale );
    return scale;
  }

  private void constrainScrollToLimits() {
    int x = getScrollX();
    int y = getScrollY();
    int constrainedX = getConstrainedScrollX( x );
    int constrainedY = getConstrainedScrollY( y );
    if( x != constrainedX || y != constrainedY ) {
      scrollTo( constrainedX, constrainedY );
    }
  }

  private void updateScaledDimensions() {
    mScaledWidth = FloatMathHelper.scale( mBaseWidth, mScale );
    mScaledHeight = FloatMathHelper.scale( mBaseHeight, mScale );
  }

  protected ZoomPanAnimator getAnimator() {
    if( mZoomPanAnimator == null ) {
      mZoomPanAnimator = new ZoomPanAnimator( this );
      mZoomPanAnimator.setDuration( mAnimationDuration );
    }
    return mZoomPanAnimator;
  }

  private int getOffsetScrollXFromScale( int offsetX, float destinationScale, float currentScale ) {
    int scrollX = getScrollX() + offsetX;
    float deltaScale = destinationScale / currentScale;
    return (int) (scrollX * deltaScale) - offsetX;
  }

  private int getOffsetScrollYFromScale( int offsetY, float destinationScale, float currentScale ) {
    int scrollY = getScrollY() + offsetY;
    float deltaScale = destinationScale / currentScale;
    return (int) (scrollY * deltaScale) - offsetY;
  }

  public void setScaleFromPosition( int offsetX, int offsetY, float scale ) {
    scale = getConstrainedDestinationScale( scale );
    if( scale == mScale ) {
      return;
    }
    int x = getOffsetScrollXFromScale( offsetX, scale, mScale );
    int y = getOffsetScrollYFromScale( offsetY, scale, mScale );

    setScale( scale );

    x = getConstrainedScrollX( x );
    y = getConstrainedScrollY( y );

    scrollTo( x, y );
  }

  @Override
  public boolean canScrollHorizontally( int direction ) {
    int position = getScrollX();
    return direction > 0 ? position < getScrollLimitX() : direction < 0 && position > 0;
  }

  @Override
  public boolean onTouchEvent( MotionEvent event ) {
    boolean gestureIntercept = mGestureDetector.onTouchEvent( event );
    boolean scaleIntercept = mScaleGestureDetector.onTouchEvent( event );
    boolean touchIntercept = mTouchUpGestureDetector.onTouchEvent( event );
    return gestureIntercept || scaleIntercept || touchIntercept || super.onTouchEvent( event );
  }

  @Override
  public void scrollTo( int x, int y ) {
    x = getConstrainedScrollX( x );
    y = getConstrainedScrollY( y );
    super.scrollTo( x, y );
  }

  private void calculateMinimumScaleToFit() {
    mMinimumScaleX = getWidth() / (float) mBaseWidth;
    mMinimumScaleY = getHeight() / (float) mBaseHeight;
    float recalculatedMinScale = calculatedMinScale(mMinimumScaleX, mMinimumScaleY);
    if( recalculatedMinScale != mEffectiveMinScale ) {
      mEffectiveMinScale = recalculatedMinScale;
      if( mScale < mEffectiveMinScale ){
        setScale( mEffectiveMinScale );
      }
    }
  }

  private float calculatedMinScale( float minimumScaleX, float minimumScaleY ) {
    switch( mMinimumScaleMode ) {
      case FILL: return Math.max( minimumScaleX, minimumScaleY );
      case FIT: return Math.min( minimumScaleX, minimumScaleY );
    }

    return mMinScale;
  }

  protected int getHalfWidth() {
    return FloatMathHelper.scale( getWidth(), 0.5f );
  }

  protected int getHalfHeight() {
    return FloatMathHelper.scale( getHeight(), 0.5f );
  }

  /**
   * When the scale is less than {@code mMinimumScaleX}, either because we are using
   * {@link MinimumScaleMode#FIT} or {@link MinimumScaleMode#NONE}, the scroll position takes a
   * value between its starting value and 0. A linear interpolation between the
   * {@code mMinimumScaleX} and the {@code mEffectiveMinScale} is used. <p>
   * This strategy is used to avoid that a custom return value of {@link #getScrollMinX} (which
   * default to 0) become the return value of this method which shifts the whole TileView.
   */
  protected int getConstrainedScrollX( int x ) {
    if ( mScale < mMinimumScaleX && mEffectiveMinScale != mMinimumScaleX ) {
      float scaleFactor = mScale / ( mMinimumScaleX - mEffectiveMinScale ) +
              mEffectiveMinScale / ( mEffectiveMinScale - mMinimumScaleX );
      return (int) ( scaleFactor * getScrollX() );
    }
    return Math.max( getScrollMinX(), Math.min( x, getScrollLimitX() ) );
  }

  /**
   * See {@link #getConstrainedScrollX(int)}
   */
  protected int getConstrainedScrollY( int y ) {
    if ( mScale < mMinimumScaleY && mEffectiveMinScale != mMinimumScaleY ) {
      float scaleFactor = mScale / ( mMinimumScaleY - mEffectiveMinScale ) +
              mEffectiveMinScale / ( mEffectiveMinScale - mMinimumScaleY );
      return (int) ( scaleFactor * getScrollY() );
    }
    return Math.max( getScrollMinY(), Math.min( y, getScrollLimitY() ) );
  }

  protected int getScrollLimitX() {
    return mScaledWidth - getWidth();
  }

  protected int getScrollLimitY() {
    return mScaledHeight - getHeight();
  }

  protected int getScrollMinX(){
    return 0;
  }

  protected int getScrollMinY(){
    return 0;
  }

  @Override
  public void computeScroll() {
    if( getScroller().computeScrollOffset() ) {
      int startX = getScrollX();
      int startY = getScrollY();
      int endX = getConstrainedScrollX( getScroller().getCurrX() );
      int endY = getConstrainedScrollY( getScroller().getCurrY() );
      if( startX != endX || startY != endY ) {
        scrollTo( endX, endY );
        if( mIsFlinging ) {
          broadcastFlingUpdate();
        }
      }
      if( getScroller().isFinished() ) {
        if( mIsFlinging ) {
          mIsFlinging = false;
          broadcastFlingEnd();
        }
      } else {
        ViewCompat.postInvalidateOnAnimation( this );
      }
    }
  }

  private void broadcastDragBegin() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanBegin( getScrollX(), getScrollY(), ZoomPanListener.Origination.DRAG );
    }
  }

  private void broadcastDragUpdate() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanUpdate( getScrollX(), getScrollY(), ZoomPanListener.Origination.DRAG );
    }
  }

  private void broadcastDragEnd() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanEnd( getScrollX(), getScrollY(), ZoomPanListener.Origination.DRAG );
    }
  }

  private void broadcastFlingBegin() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanBegin( getScroller().getStartX(), getScroller().getStartY(), ZoomPanListener.Origination.FLING );
    }
  }

  private void broadcastFlingUpdate() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanUpdate( getScroller().getCurrX(), getScroller().getCurrY(), ZoomPanListener.Origination.FLING );
    }
  }

  private void broadcastFlingEnd() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanEnd( getScroller().getFinalX(), getScroller().getFinalY(), ZoomPanListener.Origination.FLING );
    }
  }

  private void broadcastProgrammaticPanBegin() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanBegin( getScrollX(), getScrollY(), null );
    }
  }

  private void broadcastProgrammaticPanUpdate() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanUpdate( getScrollX(), getScrollY(), null );
    }
  }

  private void broadcastProgrammaticPanEnd() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanEnd( getScrollX(), getScrollY(), null );
    }
  }

  private void broadcastPinchBegin() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onZoomBegin( mScale, ZoomPanListener.Origination.PINCH );
    }
  }

  private void broadcastPinchUpdate() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onZoomUpdate( mScale, ZoomPanListener.Origination.PINCH );
    }
  }

  private void broadcastPinchEnd() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onZoomEnd( mScale, ZoomPanListener.Origination.PINCH );
    }
  }

  private void broadcastProgrammaticZoomBegin() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onZoomBegin( mScale, null );
    }
  }

  private void broadcastProgrammaticZoomUpdate() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onZoomUpdate( mScale, null );
    }
  }

  private void broadcastProgrammaticZoomEnd() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onZoomEnd( mScale, null );
    }
  }

  @Override
  public boolean onDown( MotionEvent event ) {
    if( mIsFlinging && !getScroller().isFinished() ) {
      getScroller().forceFinished( true );
      mIsFlinging = false;
      broadcastFlingEnd();
    }
    return true;
  }

  @Override
  public boolean onFling( MotionEvent event1, MotionEvent event2, float velocityX, float velocityY ) {
    getScroller().fling( getScrollX(), getScrollY(), (int) -velocityX, (int) -velocityY,
                     getScrollMinX(), getScrollLimitX(), getScrollMinY(), getScrollLimitY() );

    mIsFlinging = true;
    ViewCompat.postInvalidateOnAnimation( this );
    broadcastFlingBegin();
    return true;
  }

  @Override
  public void onLongPress( MotionEvent event ) {

  }

  @Override
  public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
    int scrollEndX = getScrollX() + (int) distanceX;
    int scrollEndY = getScrollY() + (int) distanceY;
    scrollTo( scrollEndX, scrollEndY );
    if( !mIsDragging ) {
      mIsDragging = true;
      broadcastDragBegin();
    } else {
      broadcastDragUpdate();
    }
    return true;
  }

  @Override
  public void onShowPress( MotionEvent event ) {

  }

  @Override
  public boolean onSingleTapUp( MotionEvent event ) {
    return true;
  }

  @Override
  public boolean onSingleTapConfirmed( MotionEvent event ) {
    return true;
  }

  @Override
  public boolean onDoubleTap( MotionEvent event ) {
    float destination = (float)( Math.pow( 2, Math.floor( Math.log( mScale * 2 ) / Math.log( 2 ) ) ) );
    float effectiveDestination = mShouldLoopScale && mScale >= mMaxScale ? mMinScale : destination;
    destination = getConstrainedDestinationScale( effectiveDestination );
    smoothScaleFromFocalPoint( (int) event.getX(), (int) event.getY(), destination );
    return true;
  }

  @Override
  public boolean onDoubleTapEvent( MotionEvent event ) {
    return true;
  }

  @Override
  public boolean onTouchUp( MotionEvent event ) {
    if( mIsDragging ) {
      mIsDragging = false;
      if( !mIsFlinging ) {
        broadcastDragEnd();
      }
    }
    return true;
  }

  @Override
  public boolean onScaleBegin( ScaleGestureDetector scaleGestureDetector ) {
    mIsScaling = true;
    broadcastPinchBegin();
    return true;
  }

  @Override
  public void onScaleEnd( ScaleGestureDetector scaleGestureDetector ) {
    mIsScaling = false;
    broadcastPinchEnd();
  }

  @Override
  public boolean onScale( ScaleGestureDetector scaleGestureDetector ) {
    float currentScale = mScale * mScaleGestureDetector.getScaleFactor();
    setScaleFromPosition(
      (int) scaleGestureDetector.getFocusX(),
      (int) scaleGestureDetector.getFocusY(),
      currentScale );
    broadcastPinchUpdate();
    return true;
  }

  private static class ZoomPanAnimator extends ValueAnimator implements
    ValueAnimator.AnimatorUpdateListener,
    ValueAnimator.AnimatorListener {

    private WeakReference<ZoomPanLayout> mZoomPanLayoutWeakReference;
    private ZoomPanState mStartState = new ZoomPanState();
    private ZoomPanState mEndState = new ZoomPanState();
    private boolean mHasPendingZoomUpdates;
    private boolean mHasPendingPanUpdates;

    public ZoomPanAnimator( ZoomPanLayout zoomPanLayout ) {
      super();
      addUpdateListener( this );
      addListener( this );
      setFloatValues( 0f, 1f );
      setInterpolator( new FastEaseInInterpolator() );
      mZoomPanLayoutWeakReference = new WeakReference<ZoomPanLayout>( zoomPanLayout );
    }

    private boolean setupPanAnimation( int x, int y ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if( zoomPanLayout != null ) {
        mStartState.x = zoomPanLayout.getScrollX();
        mStartState.y = zoomPanLayout.getScrollY();
        mEndState.x = x;
        mEndState.y = y;
        return mStartState.x != mEndState.x || mStartState.y != mEndState.y;
      }
      return false;
    }

    private boolean setupZoomAnimation( float scale ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if( zoomPanLayout != null ) {
        mStartState.scale = zoomPanLayout.getScale();
        mEndState.scale = scale;
        return mStartState.scale != mEndState.scale;
      }
      return false;
    }

    public void animateZoomPan( int x, int y, float scale ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if( zoomPanLayout != null ) {
        mHasPendingZoomUpdates = setupZoomAnimation( scale );
        mHasPendingPanUpdates = setupPanAnimation( x, y );
        if( mHasPendingPanUpdates || mHasPendingZoomUpdates ) {
          start();
        }
      }
    }

    public void animateZoom( float scale ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if( zoomPanLayout != null ) {
        mHasPendingZoomUpdates = setupZoomAnimation( scale );
        if( mHasPendingZoomUpdates ) {
          start();
        }
      }
    }

    public void animatePan( int x, int y ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if( zoomPanLayout != null ) {
        mHasPendingPanUpdates = setupPanAnimation( x, y );
        if( mHasPendingPanUpdates ) {
          start();
        }
      }
    }

    @Override
    public void onAnimationUpdate( ValueAnimator animation ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if( zoomPanLayout != null ) {
        float progress = (float) animation.getAnimatedValue();
        if( mHasPendingZoomUpdates ) {
          float scale = mStartState.scale + (mEndState.scale - mStartState.scale) * progress;
          zoomPanLayout.setScale( scale );
          zoomPanLayout.broadcastProgrammaticZoomUpdate();
        }
        if( mHasPendingPanUpdates ) {
          int x = (int) (mStartState.x + (mEndState.x - mStartState.x) * progress);
          int y = (int) (mStartState.y + (mEndState.y - mStartState.y) * progress);
          zoomPanLayout.scrollTo( x, y );
          zoomPanLayout.broadcastProgrammaticPanUpdate();
        }
      }
    }

    @Override
    public void onAnimationStart( Animator animator ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if( zoomPanLayout != null ) {
        if( mHasPendingZoomUpdates ) {
          zoomPanLayout.mIsScaling = true;
          zoomPanLayout.broadcastProgrammaticZoomBegin();
        }
        if( mHasPendingPanUpdates ) {
          zoomPanLayout.mIsSliding = true;
          zoomPanLayout.broadcastProgrammaticPanBegin();
        }
      }
    }

    @Override
    public void onAnimationEnd( Animator animator ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if( zoomPanLayout != null ) {
        if( mHasPendingZoomUpdates ) {
          mHasPendingZoomUpdates = false;
          zoomPanLayout.mIsScaling = false;
          zoomPanLayout.broadcastProgrammaticZoomEnd();
        }
        if( mHasPendingPanUpdates ) {
          mHasPendingPanUpdates = false;
          zoomPanLayout.mIsSliding = false;
          zoomPanLayout.broadcastProgrammaticPanEnd();
        }
      }
    }

    @Override
    public void onAnimationCancel( Animator animator ) {
      onAnimationEnd( animator );
    }

    @Override
    public void onAnimationRepeat( Animator animator ) {

    }

    private static class ZoomPanState {
      public int x;
      public int y;
      public float scale;
    }

    private static class FastEaseInInterpolator implements Interpolator {
      @Override
      public float getInterpolation( float input ) {
        return (float) (1 - Math.pow( 1 - input, 8 ));
      }
    }
  }

  public interface ZoomPanListener {
    enum Origination {
      DRAG,
      FLING,
      PINCH
    }
    void onPanBegin( int x, int y, Origination origin );
    void onPanUpdate( int x, int y, Origination origin );
    void onPanEnd( int x, int y, Origination origin );
    void onZoomBegin( float scale, Origination origin );
    void onZoomUpdate( float scale, Origination origin );
    void onZoomEnd( float scale, Origination origin );
  }

  public enum MinimumScaleMode {
    /**
     * Limit the minimum scale to no less than what
     * would be required to fill the container
     */
    FILL,

    /**
     * Limit the minimum scale to no less than what
     * would be required to fit inside the container
     */
    FIT,

    /**
     * Allow arbitrary minimum scale.
     */
    NONE
  }
}
