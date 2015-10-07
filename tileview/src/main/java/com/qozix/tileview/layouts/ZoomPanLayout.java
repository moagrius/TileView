package com.qozix.tileview.layouts;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
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
  ValueAnimator.AnimatorListener,
  ValueAnimator.AnimatorUpdateListener,
  TouchUpGestureDetector.OnTouchUpListener {

  private static final int ZOOM_ANIMATION_DURATION = 400;
  private static final int SLIDE_DURATION = 400;
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

  private boolean mIsFlinging;
  private boolean mIsDragging;
  private boolean mIsScaling;
  private boolean mIsSliding;

  private HashSet<ZoomPanListener> mZoomPanListeners = new HashSet<ZoomPanListener>();

  private ScrollActionHandler mScrollActionHandler;
  private Scroller mScroller;

  private ValueAnimator mValueAnimator;

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
   * Sets the size (width and height) of the ZoomPanLayout
   * as it should be rendered at a scale of 1f (100%).
   *
   * @param width Width of the underlying image, not the view or viewport.
   * @param height Height of the underlying image, not the view or viewport.
   */
  public void setSize( int width, int height ) {
    mBaseWidth = width;
    mBaseHeight = height;
    updateScaledDimensions();
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
    scale = Math.max( scale, mMinScale );
    scale = Math.min( scale, mMaxScale );
    if (mScale != scale) {
      float previous = mScale;
      mScale = scale;
      updateScaledDimensions();
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
    mScaledWidth = (int) (mBaseWidth * mScale);
    mScaledHeight = (int) (mBaseHeight * mScale);
  }

  /**
   * Requests a redraw
   */
  public void redraw() {
    requestLayout();
    invalidate();
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
  public boolean isDragging(){
    return mIsDragging;
  }

  /**
   * Returns whether the ZoomPanLayout is currently being slid.
   *
   * @return true if the ZoomPanLayout is currently sliding, false otherwise.
   */
  public boolean isSliding(){
    return mIsSliding;
  }

  /**
   * Returns whether the ZoomPanLayout is currently being scale tweened.
   *
   * @return true if the ZoomPanLayout is currently tweening, false otherwise.
   */
  public boolean isScaling(){
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
    mIsSliding = true;
    startWatchingScrollActions();
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
    // TODO: slideTo? center the axis that's smaller?
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
    if (mIsScaling) {
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

  private ValueAnimator getAnimator(){
    if(mValueAnimator == null) {
      mValueAnimator = ValueAnimator.ofFloat( 0, 1 );
      mValueAnimator.addListener( this );
      mValueAnimator.addUpdateListener( this );
    }
    return mValueAnimator;
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
        if(mIsFlinging){
          broadcastFlingUpdate( endX, endY );
        } else if(mIsSliding){
          broadcastProgrammaticPanUpdate( endX, endY );
        }
        startWatchingScrollActions();
      }
    }
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
    if (mIsScaling) {
      return;
    }
    ValueAnimator animator = getAnimator();
    animator.setFloatValues( mScale, destination );
    animator.setDuration( duration );
    animator.start();
  }




  private void startWatchingScrollActions(){
    mScrollActionHandler.submit();
  }
  private void onScrollerActionComplete(){
    int x = mScroller.getFinalX();
    int y = mScroller.getFinalY();
    if(mIsFlinging){
      mIsFlinging = false;
      broadcastFlingEnd( x, y );
    } else if (mIsSliding){
      mIsSliding = false;
      broadcastProgrammaticPanEnd( x, y );
    }
  }



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
      if (zoomPanLayout != null) {
        if (zoomPanLayout.getScroller().isFinished()){
          zoomPanLayout.onScrollerActionComplete();
        } else {
          submit();
        }
      }
    }
    public void clear(){
      if (hasMessages( MESSAGE )) {
        removeMessages( MESSAGE );
      }
    }
    public void submit(){
      clear();
      sendEmptyMessageDelayed( MESSAGE, FLYWHEEL_TIMEOUT );
    }
  }

  //------------------------------------------------------------------------------------
  // Public static interfaces and classes
  //------------------------------------------------------------------------------------

  private void broadcastDragBegin( int x, int y ){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onPanBegin( x, y, ZoomPanListener.Origination.DRAG );
    }
  }
  private void broadcastDragUpdate( int x, int y ){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onPanUpdate( x, y, ZoomPanListener.Origination.DRAG );
    }
  }
  private void broadcastDragEnd( int x, int y ){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onPanEnd( x, y, ZoomPanListener.Origination.DRAG );
    }
  }
  private void broadcastFlingBegin( int x, int y ){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onPanBegin( x, y, ZoomPanListener.Origination.FLING );
    }
  }
  private void broadcastFlingUpdate( int x, int y ){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onPanUpdate( x, y, ZoomPanListener.Origination.FLING );
    }
  }
  private void broadcastFlingEnd( int x, int y ){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onPanEnd( x, y, ZoomPanListener.Origination.FLING );
    }
  }
  private void broadcastProgrammaticPanBegin( int x, int y ){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onPanBegin( x, y, null );
    }
  }
  private void broadcastProgrammaticPanUpdate( int x, int y ){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onPanUpdate( x, y, null );
    }
  }
  private void broadcastProgrammaticPanEnd( int x, int y ){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onPanEnd( x, y, null );
    }
  }
  private void broadcastPinchBegin(float scale, float focusX, float focusY){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onZoomBegin( scale, focusX, focusY, ZoomPanListener.Origination.PINCH );
    }
  }
  private void broadcastPinchUpdate(float scale, float focusX, float focusY){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onZoomUpdate( scale, focusX, focusY, ZoomPanListener.Origination.PINCH );
    }
  }
  private void broadcastPinchEnd(float scale, float focusX, float focusY){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onZoomEnd( scale, focusX, focusY, ZoomPanListener.Origination.PINCH );
    }
  }
  private void broadcastProgrammaticZoomBegin( float scale, float focusX, float focusY ){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onZoomBegin( scale, focusX, focusY, null );
    }
  }
  private void broadcastProgrammaticZoomUpdate( float scale, float focusX, float focusY ){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onZoomUpdate( scale, focusX, focusY, null );
    }
  }
  private void broadcastProgrammaticZoomEnd( float scale, float focusX, float focusY ){
    for (ZoomPanListener listener : mZoomPanListeners) {
      listener.onZoomEnd( scale, focusX, focusY, null );
    }
  }

  //START OnGestureListener
  @Override
  public boolean onDown( MotionEvent event ) {
    if (!mScroller.isFinished()) {
      mScroller.abortAnimation();
      mScrollActionHandler.clear();
    }
    return true;
  }

  @Override
  public boolean onFling( MotionEvent event1, MotionEvent event2, float velocityX, float velocityY ) {
    mScroller.fling( getScrollX(), getScrollY(), (int) -velocityX, (int) -velocityY, 0, getLimitX(), 0, getLimitY() );
    mIsFlinging = true;
    startWatchingScrollActions();
    broadcastFlingBegin( getScrollX(), getScrollY() );
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
    if(!mIsDragging){
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
    saveScaleOrigination( event.getX(), event.getY() );
    startSmoothScaleTo( destination, ZOOM_ANIMATION_DURATION );
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
    if (mIsDragging) {
      mIsDragging = false;
      broadcastDragEnd( getScrollX(), getScrollY() );
    }
    return true;
  }
  //END OnTouchUpListener

  //START OnScaleGestureListener
  @Override
  public boolean onScaleBegin( ScaleGestureDetector scaleGestureDetector ) {
    float focusX = scaleGestureDetector.getFocusX();
    float focusY = scaleGestureDetector.getFocusY();
    saveScaleOrigination( focusX, focusY );
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
    setScale( currentScale );
    maintainScrollDuringScaleOperation();
    broadcastPinchUpdate( mScale, scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY() );
    return true;
  }
  //END OnScaleGestureListener

  //START AnimatorUpdateListener
  @Override
  public void onAnimationUpdate( ValueAnimator valueAnimator ) {
    float scale = (float) valueAnimator.getAnimatedValue();
    setScale( scale );
    maintainScrollDuringScaleOperation();
    broadcastProgrammaticZoomUpdate( scale, startFocusX, startFocusY );
  }
  //END AnimatorUpdateListener

  //START AnimatorListener
  @Override
  public void onAnimationStart( Animator animator ) {
    mIsScaling = true;
    broadcastProgrammaticZoomBegin( mScale, startFocusX, startFocusY );
  }

  @Override
  public void onAnimationEnd( Animator animator ) {
    mIsScaling = false;
    broadcastProgrammaticZoomEnd( mScale, startFocusX, startFocusY );
  }

  @Override
  public void onAnimationCancel( Animator animator ) {
    mIsScaling = false;
    broadcastProgrammaticZoomEnd( mScale, startFocusX, startFocusY );
  }

  @Override
  public void onAnimationRepeat( Animator animator ) {

  }
  //END AnimatorListener

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

}
