package com.qozix.tileview.widgets;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
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

  private float mEffectiveMinScale;
  private boolean mShouldScaleToFit = true;

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
    mScroller = new Scroller( context );
    mGestureDetector = new GestureDetector( context, this );
    mScaleGestureDetector = new ScaleGestureDetector( context, this );
    mTouchUpGestureDetector = new TouchUpGestureDetector( this );
  }

  @Override
  protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
    // the container's children should be the size provided by setSize
    measureChildren(
      MeasureSpec.makeMeasureSpec( mScaledWidth, MeasureSpec.EXACTLY ),  // TODO: AT_MOST
      MeasureSpec.makeMeasureSpec( mScaledHeight, MeasureSpec.EXACTLY ) );
    for( int i = 0; i < getChildCount(); i++ ){
      View child = getChildAt( i );
      LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
      layoutParams.baseWidth = mBaseWidth;
      layoutParams.baseHeight = mBaseHeight;
      layoutParams.scaledWidth = mScaledWidth;
      layoutParams.scaledHeight = mScaledHeight;
    }
    // but the container should still measure normally
    int width = MeasureSpec.getSize( widthMeasureSpec );
    int height = MeasureSpec.getSize( heightMeasureSpec );
    width = resolveSize( width, widthMeasureSpec );
    height = resolveSize( height, heightMeasureSpec );
    setMeasuredDimension( width, height );
  }

  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {
    int computedWidth = FloatMathHelper.unscale( mBaseWidth, mScale );
    int computedHeight = FloatMathHelper.unscale( mBaseHeight, mScale );
    Log.d( "TileView", "ZoomPanLayout.onLayout: " + computedWidth + ", " + computedHeight );
    for( int i = 0; i < getChildCount(); i++ ) {
      View child = getChildAt( i );
      if( child.getVisibility() != GONE ) {
        // use computed if the child scales its own canvas, no other way to make it think it's bigger (maybe clip?)
        child.layout( 0, 0, computedWidth, computedHeight );
      }
    }
    if( changed ) {
      calculateMinimumScaleToFit();
    }
  }

  /**
   * Determines whether the ZoomPanLayout should limit it's minimum scale to no less than what
   * would be required to fill it's container.
   *
   * @param shouldScaleToFit True to limit minimum scale, false to allow arbitrary minimum scale.
   */
  public void setShouldScaleToFit( boolean shouldScaleToFit ) {
    mShouldScaleToFit = shouldScaleToFit;
    calculateMinimumScaleToFit();
  }

  /**
   * Set minimum and maximum mScale values for this ZoomPanLayout.
   * Note that if shouldScaleToFit is set to true, the minimum value set here will be ignored
   * Default values are 0 and 1.
   *
   * @param min Minimum mScale the ZoomPanLayout should accept.
   * @param max Maximum mScale the ZoomPanLayout should accept.
   */
  public void setScaleLimits( float min, float max ) {
    mMinScale = min;
    mMaxScale = max;
    setScale( mScale );
  }

  /**
   * Sets the size (width and height) of the ZoomPanLayout
   * as it should be rendered at a mScale of 1f (100%).
   *
   * @param width  Width of the underlying image, not the view or viewport.
   * @param height Height of the underlying image, not the view or viewport.
   */
  public void setSize( int width, int height ) {
    mBaseWidth = width;
    mBaseHeight = height;
    updateScaledDimensions();
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

  private float getConstrainedDestinationScale( float scale ) {
    float currentMinumumScale = mShouldScaleToFit ? mEffectiveMinScale : mMinScale;
    scale = Math.max( scale, currentMinumumScale );
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

  /**
   * Sets the mScale (0-1) of the ZoomPanLayout.
   *
   * @param scale The new value of the ZoomPanLayout mScale.
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
   * Provide this method to be overriden by subclasses, e.g., onScrollChanged.
   */
  public void onScaleChanged( float currentScale, float previousScale ) {
    // noop
  }

  private void updateScaledDimensions() {
    mScaledWidth = (int) ((mBaseWidth * mScale) + 0.5);
    mScaledHeight = (int) ((mBaseHeight * mScale) + 0.5);
  }

  /**
   * Retrieves the current mScale of the ZoomPanLayout
   *
   * @return (double) the current mScale of the ZoomPanLayout
   */
  public float getScale() {
    return mScale;
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
   * Returns whether the ZoomPanLayout is currently being slid.
   *
   * @return true if the ZoomPanLayout is currently sliding, false otherwise.
   */
  public boolean isSliding() {
    return mIsSliding;
  }

  /**
   * Returns whether the ZoomPanLayout is currently being mScale tweened.
   *
   * @return true if the ZoomPanLayout is currently tweening, false otherwise.
   */
  public boolean isScaling() {
    return mIsScaling;
  }

  /**
   * Returns the Scroller instance used to manage dragging and flinging.
   *
   * @return (Scroller) The Scroller instance use to manage dragging and flinging.
   */
  public Scroller getScroller() {
    return mScroller;
  }

  /**
   * @return
   */
  public int getAnimationDuration() {
    return mAnimationDuration;
  }

  /**
   * @param animationDuration
   */
  public void setAnimationDuration( int animationDuration ) {
    mAnimationDuration = animationDuration;
    if( mZoomPanAnimator != null ) {
      mZoomPanAnimator.setDuration( mAnimationDuration );
    }
  }

  /**
   * @return
   */
  protected ZoomPanAnimator getAnimator() {
    if( mZoomPanAnimator == null ) {
      mZoomPanAnimator = new ZoomPanAnimator( this );
      mZoomPanAnimator.setDuration( mAnimationDuration );
    }
    return mZoomPanAnimator;
  }

  /**
   * Adds a ZoomPanListener to the ZoomPanLayout, which will receive events relating to zoom and pan actions.
   *
   * @param listener Listener to add.
   * @return True when the listener set did not already contain the Listener, false otherwise.
   */
  public boolean addZoomPanListener( ZoomPanListener listener ) {
    return mZoomPanListeners.add( listener );
  }

  /**
   * Removes a ZoomPanListener from the ZoomPanLayout
   *
   * @param listener Listener to remove.
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

  private int getOffsetScrollXFromScale( int offsetX, float scale ) {
    int scrollX = getScrollX() + offsetX;
    float deltaScale = scale / mScale;
    return (int) (scrollX * deltaScale) - offsetX;
  }

  private int getOffsetScrollYFromScale( int offsetY, float scale ) {
    int scrollY = getScrollY() + offsetY;
    float deltaScale = scale / mScale;
    return (int) (scrollY * deltaScale) - offsetY;
  }

  public void setScaleFromPosition( int offsetX, int offsetY, float scale ) {
    scale = getConstrainedDestinationScale( scale );
    if( scale == mScale ) {
      return;
    }
    int x = getOffsetScrollXFromScale( offsetX, scale );
    int y = getOffsetScrollYFromScale( offsetY, scale );
    scrollTo( x, y );
    setScale( scale );
  }

  /**
   * Set the scale of the ZoomPanLayout while maintaining the current center point
   *
   * @param scale The new value of the ZoomPanLayout mScale.
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
   * Scrolls and centers the ZoomPanLayout to the x and y values specified by {@param point} Point using scrolling animation
   *
   * @param x Horizontal destination point.
   * @param y Vertical destination point.
   */
  public void slideToAndCenter( int x, int y ) {
    slideTo( x - getHalfWidth(), y - getHalfHeight() );
  }

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
   * from point *on screen*, as is returned by MotionEvent.getX/Y
   *
   * @param focusX
   * @param focusY
   * @param scale
   */
  public void smoothScaleFromFocalPoint( int focusX, int focusY, float scale ) {
    scale = getConstrainedDestinationScale( scale );
    if( scale == mScale ) {
      return;
    }
    int x = getOffsetScrollXFromScale( focusX, scale );
    int y = getOffsetScrollYFromScale( focusY, scale );
    getAnimator().animateZoomPan( x, y, scale );
  }

  public void smoothScaleFromCenter( float scale ) {
    smoothScaleFromFocalPoint( getHalfWidth(), getHalfHeight(), scale );

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
    if( mShouldScaleToFit ) {
      float minimumScaleX = getWidth() / (float) mBaseWidth;
      float minimumScaleY = getHeight() / (float) mBaseHeight;
      float recalculatedMinScale = Math.max( minimumScaleX, minimumScaleY );
      if( recalculatedMinScale != mEffectiveMinScale ) {
        mEffectiveMinScale = recalculatedMinScale;
        setScale( mScale );
      }
    }
  }

  private int getHalfWidth() {
    return (int) ((getWidth() * 0.5) + 0.5);
  }

  private int getHalfHeight() {
    return (int) ((getHeight() * 0.5) + 0.5);
  }

  private int getConstrainedScrollX( int x ) {
    return Math.max( 0, Math.min( x, getScrollLimitX() ) );
  }

  private int getConstrainedScrollY( int y ) {
    return Math.max( 0, Math.min( y, getScrollLimitY() ) );
  }

  private int getScrollLimitX() {
    return mScaledWidth - getWidth();
  }

  private int getScrollLimitY() {
    return mScaledHeight - getHeight();
  }

  @Override
  public void computeScroll() {
    if( mScroller.computeScrollOffset() ) {
      int startX = getScrollX();
      int startY = getScrollY();
      int endX = getConstrainedScrollX( mScroller.getCurrX() );
      int endY = getConstrainedScrollY( mScroller.getCurrY() );
      if( startX != endX || startY != endY ) {
        scrollTo( endX, endY );
        if( mIsFlinging ) {
          broadcastFlingUpdate();
        }
      }
      if( mScroller.isFinished() ) {
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
      listener.onPanBegin( mScroller.getStartX(), mScroller.getStartY(), ZoomPanListener.Origination.FLING );
    }
  }

  private void broadcastFlingUpdate() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanUpdate( mScroller.getCurrX(), mScroller.getCurrY(), ZoomPanListener.Origination.FLING );
    }
  }

  private void broadcastFlingEnd() {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanEnd( mScroller.getFinalX(), mScroller.getFinalY(), ZoomPanListener.Origination.FLING );
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
    if( mIsFlinging && !mScroller.isFinished() ) {
      mScroller.forceFinished( true );
      broadcastFlingEnd();
    }
    return true;
  }

  @Override
  public boolean onFling( MotionEvent event1, MotionEvent event2, float velocityX, float velocityY ) {
    mScroller.fling( getScrollX(), getScrollY(), (int) -velocityX, (int) -velocityY, 0, getScrollLimitX(), 0, getScrollLimitY() );
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
    float destination = mScale >= mMaxScale ? mMinScale : mScale * 2;
    destination = getConstrainedDestinationScale( destination );
    smoothScaleFromFocalPoint( (int) event.getX(), (int) event.getY(), destination );
    return true;
  }

  @Override
  public boolean onDoubleTapEvent( MotionEvent event ) {
    return true;
  }

  @Override
  public boolean onTouchUp() {
    if( mIsDragging ) {
      mIsDragging = false;
      broadcastDragEnd();
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

  @Override
  protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0, 0, 0 );
  }

  @Override
  protected boolean checkLayoutParams( ViewGroup.LayoutParams layoutParams ) {
    return layoutParams instanceof LayoutParams;
  }

  @Override
  protected ViewGroup.LayoutParams generateLayoutParams( ViewGroup.LayoutParams layoutParams ) {
    return new LayoutParams( layoutParams );
  }

  /**
   * Per-child layout information associated with AnchorLayout.
   */
  public static class LayoutParams extends ViewGroup.LayoutParams {
    public int baseWidth;
    public int baseHeight;
    public int scaledWidth;
    public int scaledHeight;

    public LayoutParams( int width, int height, int baseWidth, int baseHeight, int scaledWidth, int scaledHeight ) {
      super( width, height );
      this.baseWidth = baseWidth;
      this.baseHeight = baseHeight;
      this.scaledWidth = scaledWidth;
      this.scaledHeight = scaledHeight;
    }

    public LayoutParams( ViewGroup.LayoutParams source ) {
      super( source );
    }
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

}
