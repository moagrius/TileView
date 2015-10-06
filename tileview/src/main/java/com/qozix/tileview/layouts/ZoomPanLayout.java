package com.qozix.tileview.layouts;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

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

public class ZoomPanLayout extends ViewGroup
  implements
  GestureDetector.OnGestureListener,
  GestureDetector.OnDoubleTapListener,
  ScaleGestureDetector.OnScaleGestureListener,
  ValueAnimator.AnimatorListener,
  ValueAnimator.AnimatorUpdateListener {

  private static final int ZOOM_ANIMATION_DURATION = 500;
  private static final int SLIDE_DURATION = 500;
  private static final int FLYWHEEL_TIMEOUT = 40;  // from AbsListView

  private int mBaseWidth;
  private int mBaseHeight;

  private int mScaledWidth;
  private int mScaledHeight;

  private float mScale = 1;
  private float mHistoricalScale = 1;

  private float mMinScale = 0;
  private float mMaxScale = 1;

  private int lastRecordedFlingX;
  private int lastRecordedFlingY;

  private int flingFinalX;
  private int flingFinalY;

  private float startFocusX;
  private float startFocusY;
  private int startScaleScrollX;
  private int startScaleScrollY;

  private boolean mShouldScaleToFit = true;

  private boolean mIsBeingFlung;
  private boolean mIsDragging;
  private boolean mIsTweening;

  private FlingHandler mFlingHandler;
  private ScrollHandler mScrollHandler;

  private Scroller mScroller;

  private HashSet<ZoomPanListener> mZoomPanListeners = new HashSet<ZoomPanListener>();

  private ValueAnimator mValueAnimator = ValueAnimator.ofFloat( 0, 1 );

  private ScaleGestureDetector mScaleGestureDetector;
  private GestureDetector mGestureDetector;


  /**
   * Constructor to use when creating a ZoomPanLayout from code.
   *
   * @param context (Context) The Context the ZoomPanLayout is running in, through which it can access the current theme, resources, etc.
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
    mFlingHandler = new FlingHandler( this );
    mScrollHandler = new ScrollHandler( this );
    mScaleGestureDetector = new ScaleGestureDetector( context, this );
    mGestureDetector = new GestureDetector( context, this );
    mValueAnimator.addListener( this );
    mValueAnimator.addUpdateListener( this );
  }

  //------------------------------------------------------------------------------------
  // PUBLIC API
  //------------------------------------------------------------------------------------

  /**
   * Determines whether the ZoomPanLayout should limit it's minimum mScale to no less than what
   * would be required to fill it's container
   *
   * @param shouldScaleToFit True to limit minimum mScale, false to allow arbitrary minimum scale.
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
   * @param min Minimum scale the ZoomPanLayout should accept.
   * @param max Maximum scale the ZoomPanLayout should accept.
   */
  public void setScaleLimits( float min, float max ) {
    // if mShouldScaleToFit is set, don't allow overwrite
    if (!mShouldScaleToFit) {
      mMinScale = min;
    }
    mMaxScale = max;
    setScale( mScale );
  }

  /**
   * Sets the size (width and height) of the ZoomPanLayout as it should be rendered at a mScale of 1f (100%)
   *
   * @param wide width
   * @param tall height
   */
  public void setSize( int wide, int tall ) {
    mBaseWidth = wide;
    mBaseHeight = tall;
    updateScaledDimensions();
  }

  /**
   * Returns the base (un-scaled) width
   *
   * @return (int) base width
   */
  public int getBaseWidth() {
    return mBaseWidth;
  }

  /**
   * Returns the base (un-scaled) height
   *
   * @return (int) base height
   */
  public int getBaseHeight() {
    return mBaseHeight;
  }

  /**
   * Returns the scaled width
   *
   * @return (int) scaled width
   */
  public int getScaledWidth() {
    return mScaledWidth;
  }

  /**
   * Returns the scaled height
   *
   * @return (int) scaled height
   */
  public int getScaledHeight() {
    return mScaledHeight;
  }

  /**
   * Sets the mScale (0-1) of the ZoomPanLayout
   *
   * @param scale The new value of the ZoomPanLayout mScale
   */
  public void setScale( float scale ) {
    scale = Math.max( scale, mMinScale );
    scale = Math.min( scale, mMaxScale );
    if (mScale != scale) {
      float previousScale = mScale;
      mScale = scale;
      updateScaledDimensions();
      invalidate();
      for (ZoomPanListener listener : mZoomPanListeners) {
        listener.onScaleChanged( mScale, previousScale );
      }
    }
  }

  private void updateScaledDimensions() {
    mScaledWidth = (int) (mBaseWidth * mScale);
    mScaledHeight = (int) (mBaseHeight * mScale);
  }

  /**
   * Requests a redraw
   */
  public void redraw() {
    requestLayout();
    postInvalidate();
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
    return mIsBeingFlung;
  }

  /**
   * Returns whether the ZoomPanLayout is currently being dragged.
   *
   * @return true if the ZoomPanLayout is currently dragging, false otherwise.
   */
  public boolean isDragging(){
    return mIsDragging;
  }

  /**
   * Returns whether the ZoomPanLayout is currently being tweened.
   *
   * @return true if the ZoomPanLayout is currently tweening, false otherwise.
   */
  public boolean isTweening(){
    return mIsTweening;
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
    int startX = getScrollX();
    int startY = getScrollY();
    int dx = x - startX;
    int dy = y - startY;

    mScroller.startScroll( startX, startY, dx, dy, SLIDE_DURATION );
    mIsBeingFlung = true;  // TODO: this is a misnomer, but required for the handler
    flingFinalX = mScroller.getFinalX();
    flingFinalY = mScroller.getFinalY(); // TODO: DRY method
    determineIfFlingComplete();
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

  /**
   * <i>This method is experimental</i>
   * Scroll and scale to match passed Rect as closely as possible.
   * The widget will attempt to frame the Rectangle, so that it's contained
   * within the viewport, if possible.
   *
   * @param rect A Rectangle instance describing the area to frame.
   */
  public void frameViewport( Rect rect ) {
    // TODO: slideTo?
    // TODO: center the axis that's smaller?
    scrollTo( rect.left, rect.top );
    float scaleX = getWidth() / (float) rect.width();
    float scaleY = getHeight() / (float) rect.height();
    float minimumScale = Math.min( scaleX, scaleY );
    smoothScaleTo( minimumScale, SLIDE_DURATION );

  }

  /**
   * Set the scale of the ZoomPanLayout while maintaining the current center point
   *
   * @param scale The new value of the ZoomPanLayout scale.
   */
  public void setScaleFromCenter( float scale ) {
    scale = Math.max( scale, mMinScale );
    scale = Math.min( scale, mMaxScale );
    if (scale == mScale) {
      return;
    }
    int offsetX = getHalfWidth();
    int offsetY = getHalfHeight();
    int scrollX = getScrollX() + offsetX;
    int scrollY = getScrollY() + offsetY;
    float deltaScale = scale / getScale();
    int x = (int) (scrollX * deltaScale) - offsetX;
    int y = (int) (scrollY * deltaScale) - offsetY;
    setScale( scale );
    scrollTo( x, y );
  }

  /**
   * Scales the ZoomPanLayout with animated progress
   *
   * @param destination (double) The final mScale to animate to
   * @param duration    (int) The duration (in milliseconds) of the animation
   */
  public void smoothScaleTo( float destination, int duration ) {
    if (mIsTweening) {
      return;
    }
    saveScaleOrigination( getHalfWidth(), getHalfHeight() );
    startSmoothScaleTo( destination, duration );
  }

  @Override
  public boolean canScrollHorizontally( int direction ) {
    int position = getScrollX();
    return direction > 0 ? position < getLimitX() : direction < 0 && position > 0;
  }

  //------------------------------------------------------------------------------------
  // PRIVATE/PROTECTED
  //------------------------------------------------------------------------------------

  // TODO: account for all cases (except wrap content)
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
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt( i );
      child.layout( 0, 0, mScaledWidth, mScaledHeight );
    }
    if (changed) {
      calculateMinimumScaleToFit();
    }
  }

  @Override
  public boolean onTouchEvent( MotionEvent event ) {
    boolean gestureIntercept = mGestureDetector.onTouchEvent( event );
    boolean scaleIntercept = mScaleGestureDetector.onTouchEvent( event );
    return gestureIntercept || scaleIntercept || super.onTouchEvent( event );
  }

  @Override
  protected void onScrollChanged (int l, int t, int oldl, int oldt){
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onScrollChanged( l, t, oldl, oldt );
    }
  }

  // TODO: update all old references to constrain, etc
  @Override
  public void scrollTo( int x, int y ) {
    x = constrainX( x );
    y = constrainY( y );
    super.scrollTo( x, y );
    //determineIfFlingComplete();
    invalidate();  // TODO: needed?
  }

  private void calculateMinimumScaleToFit() {
    if (mShouldScaleToFit) {
      float minimumScaleX = getWidth() / (float) mBaseWidth;
      float minimumScaleY = getHeight() / (float) mBaseHeight;
      float recalculatedMinScale = Math.max( minimumScaleX, minimumScaleY );
      if (recalculatedMinScale != mMinScale) {
        mMinScale = recalculatedMinScale;
        setScale( mScale );
      }
    }
  }

  private int getHalfWidth(){
    return (int) ((getWidth() * 0.5) + 0.5);
  }

  private int getHalfHeight(){
    return (int) ((getHeight() * 0.5) + 0.5);
  }

  private int constrainX( int x ) {
    return Math.max( 0, Math.min( x, getLimitX() ) );
  }

  private int constrainY( int y ) {
    return Math.max( 0, Math.min( y, getLimitY() ) );
  }

  private int getLimitX(){
    return  mScaledWidth - getWidth();
  }

  private int getLimitY(){
    return mScaledHeight - getHeight();
  }

  @Override
  public void computeScroll() {
    if (mScroller.computeScrollOffset()) {
      int startX = getScrollX();
      int startY = getScrollY();
      int endX = constrainX( mScroller.getCurrX() );
      int endY = constrainY( mScroller.getCurrY() );
      if(startX != endX || startY != endY ) {
        scrollTo( endX, endY );
        for (ZoomPanListener listener : mZoomPanListeners) {
          listener.onPan( endX, endY, ZoomPanListener.State.PROGRESS );
        }
      }
    }
    invalidate();  // TODO: need this?
  }







  private void saveScaleOrigination( float focusX, float focusY ) {
    mHistoricalScale = mScale;
    startFocusX = focusX;
    startFocusY = focusY;
    startScaleScrollX = (int) (getScrollX() + focusX);
    startScaleScrollY = (int) (getScrollY() + focusY);
  }

  private void maintainScrollDuringScaleOperation() {
    double deltaScale = mScale / mHistoricalScale;
    int x = (int) ((startScaleScrollX * deltaScale) - startFocusX);
    int y = (int) ((startScaleScrollY * deltaScale) - startFocusY);
    scrollTo( x, y );
  }

  private void startSmoothScaleTo( float destination, int duration ) {
    if (mIsTweening) {
      return;
    }
    mValueAnimator.setFloatValues( mScale, destination );
    mValueAnimator.setDuration( duration );
    mValueAnimator.start();
  }





  private int lastRecordedScrollX;
  private int lastRecordedScrollY;
  private void recordScrollState(){
    lastRecordedScrollX = getScrollX();
    lastRecordedScrollY = getScrollY();
  }
  private void notifyScrollHasStoppedEnough(){
    for( ZoomPanListener listener : mZoomPanListeners ) {
      listener.onPan( getScrollX(), getScrollY(), ZoomPanListener.State.COMPLETE );
    }
  }
  private boolean determineIfScrollHasChanged(){
    return getScrollX() != lastRecordedScrollX || getScrollY() != lastRecordedScrollY;
  }
  private static class ScrollHandler extends Handler {
    private static final int MESSAGE = 0;
    private boolean isCompleteEnough = false;
    private final WeakReference<ZoomPanLayout> mZoomPanLayoutWeakReference;
    public ScrollHandler( ZoomPanLayout zoomPanLayout ) {
      super();
      mZoomPanLayoutWeakReference = new WeakReference<ZoomPanLayout>( zoomPanLayout );
    }
    @Override
    public void handleMessage( Message msg ) {
      Log.d( "TileView", "handleMessage" );
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if (zoomPanLayout != null) {
        if (zoomPanLayout.determineIfScrollHasChanged()){
          Log.d( "TileView", "submit" );
          zoomPanLayout.recordScrollState();
          submit();
        } else {
          zoomPanLayout.notifyScrollHasStoppedEnough();
        }
      }
    }
    public void clear(){
      Log.d( "TileView", "clear" );
      if (hasMessages( MESSAGE )) {
        removeMessages( MESSAGE );
      }
    }
    public void submit(){
      Log.d( "TileView", "submit" );
      clear();
      sendEmptyMessageDelayed( MESSAGE, FLYWHEEL_TIMEOUT );
    }
  }










  private void determineIfFlingComplete() {
    if(mIsBeingFlung) {
      if (mFlingHandler.hasMessages( 0 )) {
        mFlingHandler.removeMessages( 0 );
      }
      int x = getScrollX();
      int y = getScrollY();
      // if scroll position is same as last recorded, assume we're done with the fling

      // SEPARATE THIS OUT
      // fling needs to check against final values
      // drag should not need to run a handler WRONG
      // slide to should stop in animationComplete
      // LATEST IDEA - run everything thing a single onScrollChanged handler
      if( x == flingFinalX && y == flingFinalY ){  // TODO: mScroller.isFinished()?
        mIsBeingFlung = false;
        flingComplete();
      } else {
        mFlingHandler.sendEmptyMessageDelayed( 0, FLYWHEEL_TIMEOUT );
      }
    }
  }

  private void flingComplete() {
    if (mIsBeingFlung) {
      mIsBeingFlung = false;
      for (ZoomPanListener listener : mZoomPanListeners) {
        listener.onPan( getScrollX(), getScrollY(), ZoomPanListener.State.COMPLETE );
      }
    }
    invalidate();  // TODO: need this?
  }

  private static class FlingHandler extends Handler {
    private final WeakReference<ZoomPanLayout> mZoomPanLayoutWeakReference;
    public FlingHandler( ZoomPanLayout zoomPanLayout ) {
      super();
      mZoomPanLayoutWeakReference = new WeakReference<ZoomPanLayout>( zoomPanLayout );
    }
    @Override
    public void handleMessage( Message msg ) {
      ZoomPanLayout zoomPanLayout = mZoomPanLayoutWeakReference.get();
      if (zoomPanLayout != null) {
        zoomPanLayout.determineIfFlingComplete();
      }
    }
  }

  //------------------------------------------------------------------------------------
  // Public static interfaces and classes
  //------------------------------------------------------------------------------------





  public interface ZoomPanListener {
    enum State {
      STARTED,
      COMPLETE,
      PROGRESS
    }
    void onPan( int x, int y, State state);
    void onZoom( float scale, float focusX, float focusY, State state );
    void onScaleChanged( float currentScale, float previousScale );
    void onScrollChanged( int currentX, int currentY, int previousX, int previousY );
  }



  //START OnGestureListener
  @Override
  public boolean onDown( MotionEvent event ) {
    if (!mScroller.isFinished()) {
      mScroller.abortAnimation();
      // TODO: kill handler
    }
    return true;
  }

  @Override
  public boolean onFling( MotionEvent event1, MotionEvent event2, float velocityX, float velocityY ) {
    mIsBeingFlung = true;  // TODO: put save state in a method
    mScroller.fling( getScrollX(), getScrollY(), (int) -velocityX, (int) -velocityY, 0, getLimitX(), 0, getLimitY() );
    // TODO: check current scroll to finalPoint to determine end
    flingFinalX = mScroller.getFinalX();
    flingFinalY = mScroller.getFinalY();
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onPan( getScrollX(), getScrollY(), ZoomPanListener.State.STARTED );
    }
    determineIfFlingComplete();
    return true;
  }

  @Override
  public void onLongPress( MotionEvent event ) {

  }

  @Override
  public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
    int scrollEndX = (int) (getScrollX() + distanceX);
    int scrollEndY = (int) (getScrollY() + distanceY);
    scrollTo( scrollEndX, scrollEndY );
    mScrollHandler.submit();
    ZoomPanListener.State state = mIsDragging ? ZoomPanListener.State.PROGRESS : ZoomPanListener.State.STARTED;
    if(!mIsDragging){
      mIsDragging = true;
    }
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onPan( scrollEndX, scrollEndY, ZoomPanListener.State.STARTED );
    }
    return true;
  }

  @Override
  public void onShowPress( MotionEvent event ) {

  }

  /*
  TODO: do we want to stop drag here, and just pipe renders during drags?  it's more solid
  (the complete event would really be complete), but the render requests might add up.
  leaning toward do it.
   */
  @Override
  public boolean onSingleTapUp( MotionEvent event ) {
    if(mIsDragging){
      mIsDragging = false;
      for (ZoomPanListener listener : mZoomPanListeners) {
        listener.onPan( getScrollX(), getScrollY(), ZoomPanListener.State.COMPLETE );
      }
    }
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
    saveScaleOrigination( event.getX(), event.getY() );
    startSmoothScaleTo( destination, ZOOM_ANIMATION_DURATION );
    return true;
  }

  @Override
  public boolean onDoubleTapEvent( MotionEvent event ) {
    return true;
  }
  //END OnDoubleTapListener

  //START OnScaleGestureListener
  @Override
  public boolean onScaleBegin( ScaleGestureDetector scaleGestureDetector ) {
    float focusX = scaleGestureDetector.getFocusX();
    float focusY = scaleGestureDetector.getFocusY();
    saveScaleOrigination( focusX, focusY );
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onZoom( mScale, focusX, focusY, ZoomPanListener.State.STARTED );
    }
    return true;
  }

  @Override
  public void onScaleEnd( ScaleGestureDetector scaleGestureDetector ) {
    int focusX = (int) scaleGestureDetector.getFocusX();
    int focusY = (int) scaleGestureDetector.getFocusY();
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onZoom( mScale, focusX, focusY, ZoomPanListener.State.COMPLETE );
    }
  }

  @Override
  public boolean onScale( ScaleGestureDetector scaleGestureDetector ) {
    float currentScale = mScale * mScaleGestureDetector.getScaleFactor();
    setScale( currentScale );
    maintainScrollDuringScaleOperation();
    int focusX = (int) scaleGestureDetector.getFocusX();
    int focusY = (int) scaleGestureDetector.getFocusY();
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onZoom( mScale, focusX, focusY, ZoomPanListener.State.PROGRESS );
    }
    return true;
  }
  //END OnScaleGestureListener

  //START AnimatorUpdateListener
  @Override
  public void onAnimationUpdate( ValueAnimator valueAnimator ) {
    float scale = (float) valueAnimator.getAnimatedValue();
    setScale( scale );
    maintainScrollDuringScaleOperation();
    // TODO: update startFocusX/Y?
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onZoom( scale, startFocusX, startFocusY, ZoomPanListener.State.PROGRESS );
    }
  }
  //END AnimatorUpdateListener

  //START AnimatorListener
  @Override
  public void onAnimationStart( Animator animator ) {
    mIsTweening = true;
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onZoom( mScale, startFocusX, startFocusY, ZoomPanListener.State.STARTED );
    }
  }

  @Override
  public void onAnimationEnd( Animator animator ) {
    mIsTweening = false;
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onZoom( mScale, startFocusX, startFocusY, ZoomPanListener.State.COMPLETE );
    }
  }

  @Override
  public void onAnimationCancel( Animator animator ) {

  }

  @Override
  public void onAnimationRepeat( Animator animator ) {

  }
  //END AnimatorListener

}
