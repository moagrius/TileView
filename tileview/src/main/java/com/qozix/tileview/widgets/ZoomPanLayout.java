package com.qozix.tileview.widgets;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
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

import com.qozix.tileview.view.TouchUpGestureDetector;

import java.lang.ref.WeakReference;
import java.util.HashSet;

/**
 * ZoomPanLayout extends ViewGroup to provide support for scrolling and zooming.
 * Fling, drag, pinch and double-tap events are supported natively.
 *
 * Children of ZoomPanLayout are positioned as if provided
 * with LayoutParams of MATCH_PARENT for both axes,
 * and will always be positioned at 0,0.
 */

public class ZoomPanLayout extends ViewGroup implements
  GestureDetector.OnGestureListener,
  GestureDetector.OnDoubleTapListener,
  ScaleGestureDetector.OnScaleGestureListener,
  TouchUpGestureDetector.OnTouchUpListener {

  protected static final int ZOOM_ANIMATION_DURATION = 400;
  // TODO: consolidate, and allow setable
  protected static final int SLIDE_DURATION = 400;
  protected static final int FLYWHEEL_TIMEOUT = 40;  // from AbsListView

  private int mBaseWidth;
  private int mBaseHeight;

  private int mScaledWidth;
  private int mScaledHeight;

  private float mScale = 1;

  private float mMinScale = 0;
  private float mMaxScale = 1;

  private boolean mShouldScaleToFit = true;  // TODO:

  private boolean mIsFlinging;
  private boolean mIsDragging;
  private boolean mIsScaling;
  private boolean mIsSliding;

  private HashSet<ZoomPanListener> mZoomPanListeners = new HashSet<ZoomPanListener>();

  private ScrollActionHandler mScrollActionHandler;
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
    mScrollActionHandler = new ScrollActionHandler( this );
    mGestureDetector = new GestureDetector( context, this );
    mScaleGestureDetector = new ScaleGestureDetector( context, this );
    mTouchUpGestureDetector = new TouchUpGestureDetector( this );
  }

  //------------------------------------------------------------------------------------
  // PUBLIC API
  //------------------------------------------------------------------------------------

  /**
   * Determines whether the ZoomPanLayout should limit it's minimum mScale to no less than what
   * would be required to fill it's container.
   *
   * @param shouldScaleToFit True to limit minimum mScale, false to allow arbitrary minimum mScale.
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
    // if mShouldScaleToFit is set, don't allow overwrite
    if( !mShouldScaleToFit ) {
      mMinScale = min;
    }
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
    scale = Math.max( scale, mMinScale );
    scale = Math.min( scale, mMaxScale );
    return scale;
  }

  private void constrainScrollToLimits(){
    int x = getScrollX();
    int y = getScrollY();
    int constrainedX = constrainX( x );
    int constrainedY = constrainY( y );
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
      onScaleChanged( scale, previous );
      constrainScrollToLimits();
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

  protected ZoomPanAnimator getAnimator(){
    if( mZoomPanAnimator == null ) {
      mZoomPanAnimator = new ZoomPanAnimator( this );
    }
    return mZoomPanAnimator;
  }

  /**
   * Adds a ZoomPanListener to the ZoomPanLayout, which will receive events relating to zoom and pan actions
   *
   * @param listener (ZoomPanListener) Listener to add
   * @return (boolean) true when the listener set did not already contain the Listener, false otherwise
   */
  public boolean addZoomPanListener( ZoomPanListener listener ) {
    return mZoomPanListeners.add( listener );
  }

  /**
   * Removes a ZoomPanListener from the ZoomPanLayout
   *
   * @param listener (ZoomPanListener) Listener to remove
   * @return (boolean) if the Listener was removed, false otherwise
   */
  public boolean removeZoomPanListener( ZoomPanListener listener ) {
    return mZoomPanListeners.remove( listener );
  }

  /**
   * Scrolls and centers the ZoomPanLayout to the x and y values specified by {@param point} Point
   *
   * @param x Horizontal destination point.
   * @param y Vertical destination point.
   */
  public void scrollToAndCenter( int x, int y ) {
    scrollTo( x - getHalfWidth(), y - getHalfHeight() );
  }

  /**
   * Scrolls the ZoomPanLayout to the x and y values provided using scrolling animation.
   *
   * @param x Horizontal destination point.
   * @param y Vertical destination point.
   */
  public void slideTo( int x, int y ) {
    mIsSliding = true;
    getAnimator().animate( x, y, mScale );

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
    getAnimator().animate( x - getHalfWidth(), y - getHalfHeight(), scale );
  }

  /**
   * Set the mScale of the ZoomPanLayout while maintaining the current center point
   *
   * @param scale The new value of the ZoomPanLayout mScale.
   */
  public void setScaleFromCenter( float scale ) {
    int offsetX = getHalfWidth();
    int offsetY = getHalfHeight();
    setScaleFromPosition( offsetX, offsetY, scale );
  }

  public void setScaleFromPosition( int offsetX, int offsetY, float scale ) {
    scale = Math.max( scale, mMinScale );
    scale = Math.min( scale, mMaxScale );
    if( scale == mScale ) {
      return;
    }
    int scrollX = getScrollX() + offsetX;
    int scrollY = getScrollY() + offsetY;
    float deltaScale = scale / getScale();
    int x = (int) (scrollX * deltaScale) - offsetX;
    int y = (int) (scrollY * deltaScale) - offsetY;
    scrollTo( x, y );
    setScale( scale );
  }

  /**
   * Scales the ZoomPanLayout with animated progress
   *
   */
  public void smoothScaleTo( float destination ) {
    if( mIsScaling ) {
      return;
    }
    getAnimator().animate( getScrollX(), getScrollY(), destination );
  }

  public void smoothScaleFromCenter( float scale ) {
    int offsetX = getHalfWidth();
    int offsetY = getHalfHeight();
    smoothScaleFromLocation( offsetX, offsetY, scale );

  }

  /**
   * from point *on screen*, as is returned by MotionEvent.getX/Y
   * @param offsetX
   * @param offsetY
   * @param scale
   */
  public void smoothScaleFromLocation( int offsetX, int offsetY, float scale ) {
    scale = Math.max( scale, mMinScale );
    scale = Math.min( scale, mMaxScale );
    if( scale == mScale ) {
      return;
    }
    int scrollX = getScrollX() + offsetX;
    int scrollY = getScrollY() + offsetY;
    float deltaScale = scale / getScale();
    int x = (int) (scrollX * deltaScale) - offsetX;
    int y = (int) (scrollY * deltaScale) - offsetY;
    getAnimator().animate( x, y, scale );
  }

  @Override
  public boolean canScrollHorizontally( int direction ) {
    int position = getScrollX();
    return direction > 0 ? position < getLimitX() : direction < 0 && position > 0;
  }

  //------------------------------------------------------------------------------------
  // PRIVATE/PROTECTED
  //------------------------------------------------------------------------------------

  @Override
  protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
    measureChildren( widthMeasureSpec, heightMeasureSpec );
    int width = MeasureSpec.getSize( widthMeasureSpec );
    int height = MeasureSpec.getSize( heightMeasureSpec );
    width = Math.max( width, getSuggestedMinimumWidth() );
    height = Math.max( height, getSuggestedMinimumHeight() );
    width = resolveSize( width, widthMeasureSpec );
    height = resolveSize( height, heightMeasureSpec );
    setMeasuredDimension( width, height );
  }

  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {
    for( int i = 0; i < getChildCount(); i++ ) {
      View child = getChildAt( i );
      if( child.getVisibility() != GONE ) {
        child.layout( 0, 0, mScaledWidth, mScaledHeight );
      }
    }
    if( changed ) {
      calculateMinimumScaleToFit();
    }
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
    x = constrainX( x );
    y = constrainY( y );
    super.scrollTo( x, y );
  }

  private void calculateMinimumScaleToFit() {
    if( mShouldScaleToFit ) {
      float minimumScaleX = getWidth() / (float) mBaseWidth;
      float minimumScaleY = getHeight() / (float) mBaseHeight;
      float recalculatedMinScale = Math.max( minimumScaleX, minimumScaleY );
      if( recalculatedMinScale != mMinScale ) {
        mMinScale = recalculatedMinScale;
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

  private int constrainX( int x ) {
    return Math.max( 0, Math.min( x, getLimitX() ) );
  }

  private int constrainY( int y ) {
    return Math.max( 0, Math.min( y, getLimitY() ) );
  }

  private int getLimitX() {
    return mScaledWidth - getWidth();
  }

  private int getLimitY() {
    return mScaledHeight - getHeight();
  }

  @Override
  public void computeScroll() {
    if( mScroller.computeScrollOffset() ) {
      int startX = getScrollX();
      int startY = getScrollY();
      int endX = constrainX( mScroller.getCurrX() );
      int endY = constrainY( mScroller.getCurrY() );
      if( startX != endX || startY != endY ) {
        scrollTo( endX, endY );
        if( mIsFlinging ) {
          broadcastFlingUpdate( endX, endY );
        }
      }
      if( !mScroller.isFinished()){
        ViewCompat.postInvalidateOnAnimation( this );
      }
    }
  }

  private void concludeDrag() {
    if( mIsDragging ) {
      mIsDragging = false;
      broadcastDragEnd( getScrollX(), getScrollY() );
    }
  }

  private void concludeFling() {
    if( mIsFlinging ) {
      mIsFlinging = false;
      mScroller.forceFinished( true );
      mScrollActionHandler.clear();
      broadcastFlingEnd( getScrollX(), getScrollY() );
    }
  }

  private void concludeSlide() {
    if( mIsSliding ) {
      mIsSliding = false;
      mScroller.forceFinished( true );
      mScrollActionHandler.clear();
      broadcastProgrammaticPanEnd( getScrollX(), getScrollY() );
    }
  }

  private boolean isScrollActionComplete() {
    return mScroller.isFinished();
  }

  private void startWatchingScrollActions() {
    mScrollActionHandler.submit();
  }

  private void onScrollerActionComplete() {
    if( mIsFlinging ) {
      concludeFling();
    }
    if( mIsSliding ) {
      concludeSlide();
    }
    mScrollActionHandler.clear();
  }



  //------------------------------------------------------------------------------------
  // Convenience dispatch methods
  //------------------------------------------------------------------------------------

  private void broadcastDragBegin( int x, int y ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanBegin( x, y, ZoomPanListener.Origination.DRAG );
    }
  }

  private void broadcastDragUpdate( int x, int y ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanUpdate( x, y, ZoomPanListener.Origination.DRAG );
    }
  }

  private void broadcastDragEnd( int x, int y ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanEnd( x, y, ZoomPanListener.Origination.DRAG );
    }
  }

  private void broadcastFlingBegin( int x, int y ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanBegin( x, y, ZoomPanListener.Origination.FLING );
    }
  }

  private void broadcastFlingUpdate( int x, int y ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanUpdate( x, y, ZoomPanListener.Origination.FLING );
    }
  }

  private void broadcastFlingEnd( int x, int y ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanEnd( x, y, ZoomPanListener.Origination.FLING );
    }
  }

  // TODO:
  private void broadcastProgrammaticPanBegin( int x, int y ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanBegin( x, y, null );
    }
  }

  private void broadcastProgrammaticPanUpdate( int x, int y ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanUpdate( x, y, null );
    }
  }

  private void broadcastProgrammaticPanEnd( int x, int y ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPanEnd( x, y, null );
    }
  }

  private void broadcastPinchBegin( float scale, float focusX, float focusY ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onZoomBegin( scale, focusX, focusY, ZoomPanListener.Origination.PINCH );
    }
  }

  private void broadcastPinchUpdate( float scale, float focusX, float focusY ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onZoomUpdate( scale, focusX, focusY, ZoomPanListener.Origination.PINCH );
    }
  }

  private void broadcastPinchEnd( float scale, float focusX, float focusY ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onZoomEnd( scale, focusX, focusY, ZoomPanListener.Origination.PINCH );
    }
  }

  private void broadcastProgrammaticZoomBegin( float scale, float focusX, float focusY ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onZoomBegin( scale, focusX, focusY, null );
    }
  }

  private void broadcastProgrammaticZoomUpdate( float scale, float focusX, float focusY ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onZoomUpdate( scale, focusX, focusY, null );
    }
  }

  private void broadcastProgrammaticZoomEnd( float scale, float focusX, float focusY ) {
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onZoomEnd( scale, focusX, focusY, null );
    }
  }

  //------------------------------------------------------------------------------------
  // end convenience dispatch methods
  //------------------------------------------------------------------------------------

  //------------------------------------------------------------------------------------
  // Hooks - override at will in subclasses
  //------------------------------------------------------------------------------------

  //START OnGestureListener
  @Override
  public boolean onDown( MotionEvent event ) {
    if( !mScroller.isFinished() ) {
      concludeFling();
      concludeSlide();
    }
    return true;
  }

  @Override
  public boolean onFling( MotionEvent event1, MotionEvent event2, float velocityX, float velocityY ) {
    mScroller.fling( getScrollX(), getScrollY(), (int) -velocityX, (int) -velocityY, 0, getLimitX(), 0, getLimitY() );
    mIsFlinging = true;
    broadcastFlingBegin( getScrollX(), getScrollY() );
    ViewCompat.postInvalidateOnAnimation( this );
    startWatchingScrollActions();
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
      broadcastDragBegin( scrollEndX, scrollEndY );
    } else {
      broadcastDragUpdate( scrollEndX, scrollEndY );
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
  //END OnGestureListener

  //START OnDoubleTapListener
  @Override
  public boolean onSingleTapConfirmed( MotionEvent event ) {
    return true;
  }

  @Override
  public boolean onDoubleTap( MotionEvent event ) {
    float destination = mScale >= mMaxScale ? mMinScale : Math.min( mMaxScale, mScale * 2 );
    smoothScaleFromLocation( (int) event.getX(), (int) event.getY(), destination );
    return true;
  }

  @Override
  public boolean onDoubleTapEvent( MotionEvent event ) {
    return true;
  }
  //END OnDoubleTapListener

  //START OnTouchUpListener
  @Override
  public boolean onTouchUp() {
    concludeDrag();
    return true;
  }
  //END OnTouchUpListener

  //START OnScaleGestureListener
  @Override
  public boolean onScaleBegin( ScaleGestureDetector scaleGestureDetector ) {
    float focusX = scaleGestureDetector.getFocusX();
    float focusY = scaleGestureDetector.getFocusY();
    broadcastPinchBegin( mScale, focusX, focusY );
    return true;
  }

  @Override
  public void onScaleEnd( ScaleGestureDetector scaleGestureDetector ) {
    broadcastPinchEnd( mScale, scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY() );
  }

  @Override
  public boolean onScale( ScaleGestureDetector scaleGestureDetector ) {
    float currentScale = mScale * mScaleGestureDetector.getScaleFactor();
    setScaleFromPosition(
      (int) scaleGestureDetector.getFocusX(),
      (int) scaleGestureDetector.getFocusY(),
      currentScale );
    broadcastPinchUpdate( mScale, scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY() );
    return true;
  }
  //END OnScaleGestureListener


  //------------------------------------------------------------------------------------
  // end hooks
  //------------------------------------------------------------------------------------

  //------------------------------------------------------------------------------------
  // Helper classes (internal, private)
  //------------------------------------------------------------------------------------

  /**
   * Handler will start dispatching messages when a fling or slide action is initiated,
   * then continue to poll until the action is complete when it notifies the associate
   * ZoomPanLayout instance.
   */
  private static class ScrollActionHandler extends Handler {
    private static final int MESSAGE = 0;
    private final WeakReference<ZoomPanLayout> mZoomPanLayoutWeakReference;

    public ScrollActionHandler( ZoomPanLayout zoomPanLayout ) {
      super();
      mZoomPanLayoutWeakReference = new WeakReference<ZoomPanLayout>( zoomPanLayout );
    }

    @Override
    public void handleMessage( Message msg ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if( zoomPanLayout != null ) {
        if( zoomPanLayout.isScrollActionComplete() ) {
          zoomPanLayout.onScrollerActionComplete();
        } else {
          submit();
        }
      }
    }

    public void clear() {
      if( hasMessages( MESSAGE ) ) {
        removeMessages( MESSAGE );
      }
    }

    public void submit() {
      clear();
      sendEmptyMessageDelayed( MESSAGE, FLYWHEEL_TIMEOUT );
    }
  }

  // TODO: new animator instance per call?  or no calls while mIsAnimating?
  // TODO: leverage a scroller for everything?  bleh @ pure scale calls
  private static class ZoomPanAnimator extends ValueAnimator implements
    ValueAnimator.AnimatorUpdateListener,
    ValueAnimator.AnimatorListener {
    // TODO: settable?
    private static final int DEFAULT_ZOOM_PAN_ANIMATION_DURATION = 400;  // per Scroller
    private WeakReference<ZoomPanLayout> mZoomPanLayoutWeakReference;
    private ZoomPanState mStartState = new ZoomPanState();
    private ZoomPanState mEndState = new ZoomPanState();
    private boolean mIsMutatingScale;
    private boolean mIsMutatingScroll;
    public ZoomPanAnimator( ZoomPanLayout zoomPanLayout ) {
      super();
      setDuration( DEFAULT_ZOOM_PAN_ANIMATION_DURATION );
      addUpdateListener( this );
      addListener( this );
      setFloatValues( 0f, 1f );
      setInterpolator( new ViscousFluidInterpolator() );
      mZoomPanLayoutWeakReference = new WeakReference<ZoomPanLayout>( zoomPanLayout );
      mZoomPanLayoutWeakReference = new WeakReference<ZoomPanLayout>( zoomPanLayout );
    }
    public void animate( int x, int y, float scale ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if( zoomPanLayout != null ) {
        mStartState.scale = zoomPanLayout.getScale();
        mStartState.scrollX = zoomPanLayout.getScrollX();
        mStartState.scrollY = zoomPanLayout.getScrollY();
        mEndState.scale = scale;
        mEndState.scrollX = x;
        mEndState.scrollY = y;
        mIsMutatingScale = mStartState.scale != mEndState.scale;
        mIsMutatingScroll = mStartState.scrollX != mEndState.scrollX
          || mStartState.scrollY != mEndState.scrollY;
        start();
      }
    }
    @Override
    public void onAnimationUpdate( ValueAnimator animation ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if( zoomPanLayout != null ) {
        float progress = (float) animation.getAnimatedValue();
        int scrollX = (int) (mStartState.scrollX + ( mEndState.scrollX - mStartState.scrollX) * progress);
        int scrollY = (int) (mStartState.scrollY + ( mEndState.scrollY - mStartState.scrollY) * progress);
        float scale = mStartState.scale + ( mEndState.scale - mStartState.scale) * progress;
        zoomPanLayout.setScale( scale );
        zoomPanLayout.scrollTo( scrollX, scrollY );
      }
    }
    @Override
    public void onAnimationStart( Animator animator ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if( zoomPanLayout != null ) {
        zoomPanLayout.mIsScaling = true;
        zoomPanLayout.broadcastProgrammaticZoomBegin( zoomPanLayout.mScale, 0, 0 );
      }
    }
    @Override
    public void onAnimationEnd( Animator animator ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if( zoomPanLayout != null ) {
        zoomPanLayout.mIsScaling = false;  // TODO: broadcast changes
        Log.d( "Anim", "should be broadcasting programmatic zoom end" );
        zoomPanLayout.broadcastProgrammaticZoomEnd( zoomPanLayout.mScale, 0, 0 );
      }
    }
    @Override
    public void onAnimationCancel( Animator animator ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if( zoomPanLayout != null ) {
        zoomPanLayout.mIsScaling = false; // TODO: startFocusX still being used?
        zoomPanLayout.broadcastProgrammaticZoomEnd( zoomPanLayout.mScale, 0, 0 );
      }
    }
    @Override
    public void onAnimationRepeat( Animator animator ) {

    }
    private static class ZoomPanState {
      public int scrollX;
      public int scrollY;
      public float scale;
    }
    private static class ViscousFluidInterpolator implements Interpolator {
      private static final float VISCOUS_FLUID_SCALE = 8.0f;
      private static final float VISCOUS_FLUID_NORMALIZE;
      private static final float VISCOUS_FLUID_OFFSET;
      static {
        VISCOUS_FLUID_NORMALIZE = 1.0f / viscousFluid(1.0f);
        VISCOUS_FLUID_OFFSET = 1.0f - VISCOUS_FLUID_NORMALIZE * viscousFluid(1.0f);
      }
      private static float viscousFluid(float x) {
        x *= VISCOUS_FLUID_SCALE;
        if (x < 1.0f) {
          x -= (1.0f - (float)Math.exp(-x));
        } else {
          float start = 0.36787944117f;   // 1/e == exp(-1)
          x = 1.0f - (float)Math.exp(1.0f - x);
          x = start + x * (1.0f - start);
        }
        return x;
      }

      @Override
      public float getInterpolation(float input) {
        final float interpolated = VISCOUS_FLUID_NORMALIZE * viscousFluid(input);
        if (interpolated > 0) {
          return interpolated + VISCOUS_FLUID_OFFSET;
        }
        return interpolated;
      }
    }
  }

  //------------------------------------------------------------------------------------
  // end helper classes (internal, private)
  //------------------------------------------------------------------------------------

  //------------------------------------------------------------------------------------
  // Listener interfaces
  //------------------------------------------------------------------------------------

  public interface ZoomPanListener {
    enum Origination {
      DRAG,
      FLING,
      PINCH
    }
    void onPanBegin( int x, int y, Origination origin );
    void onPanUpdate( int x, int y, Origination origin );
    void onPanEnd( int x, int y, Origination origin );
    void onZoomBegin( float scale, float focusX, float focusY, Origination origin );
    void onZoomUpdate( float scale, float focusX, float focusY, Origination origin );
    void onZoomEnd( float scale, float focusX, float focusY, Origination origin );
  }

  //------------------------------------------------------------------------------------
  // end listener interfaces
  //------------------------------------------------------------------------------------

}
