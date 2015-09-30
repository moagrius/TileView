package com.qozix.tileview.layouts;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;


import java.lang.ref.WeakReference;
import java.util.HashSet;

/**
 * ZoomPanLayout extends ViewGroup to provide support for scrolling and zooming.  Fling, drag, pinch and
 * double-tap events are supported natively.
 * 
 * ZoomPanLayout does not support direct insertion of child Views, and manages positioning through an intermediary View.
 * the addChild method provides an interface to add layouts to that intermediary view.  Each of these children are provided
 * with LayoutParams of MATCH_PARENT for both axes, and will always be positioned at 0,0, so should generally be ViewGroups
 * themselves (RelativeLayouts or FrameLayouts are generally appropriate).
 */

public class ZoomPanLayout extends ViewGroup {

	private static final int MINIMUM_VELOCITY = 50;
	private static final int ZOOM_ANIMATION_DURATION = 500;
	private static final int SLIDE_DURATION = 500;
	private static final int VELOCITY_UNITS = 1000;
	private static final int DOUBLE_TAP_TIME_THRESHOLD = 250;
	private static final int SINGLE_TAP_DISTANCE_THRESHOLD = 50;
	private static final double MINIMUM_PINCH_SCALE = 0.0;

	private int mBaseWidth;
	private int mBaseHeight;

	private int mScaledWidth;
	private int mScaledHeight;

	private double scale = 1;
	private double mHistoricalScale = 1;

	private double mMinScale = 0;
	private double mMaxScale = 1;

	private boolean mShouldScaleToFit = true;

	private Point mPinchStartScroll = new Point();
	private Point mPinchStartOffset = new Point();
	private double mPinchStartDistance;

	private Point mDoubleTapStartScroll = new Point();
	private Point mDoubleTapStartOffset = new Point();
	private double mDoubleTapDestinationScale;

	private Point mFirstFinger = new Point();
	private Point mSecondFinger = new Point();
	private Point mLastFirstFinger = new Point();
	private Point mLastSecondFinger = new Point();

	private Point mScrollPosition = new Point();

	private Point mSingleTapHistory = new Point();
	private Point mDoubleTapHistory = new Point();

	private Point mFirstFingerLastDown = new Point();
	private Point mSecondFingerLastDown = new Point();
	
	private Point mActualPoint = new Point();
	private Point mDestinationScroll = new Point();

	private boolean mSecondFingerIsDown = false;
	private boolean mFirstFingerIsDown = false;

	private boolean mIsTapInterrupted = false;
	private boolean mIsBeingFlung = false;
	private boolean mIsDragging = false;
	private boolean mIsPinching = false;

	private int mDragStartThreshold = 30;
	private int mPinchStartThreshold = 30;

	private long mLastTouchedAt;

	private ScrollActionHandler mScrollActionHandler;

	private Scroller mScroller;
	private VelocityTracker mVelocityTracker;

	private HashSet<GestureListener> mGestureListeners = new HashSet<GestureListener>();
	private HashSet<ZoomPanListener> mZoomPanListeners = new HashSet<ZoomPanListener>();

	private StaticLayout mClip;

	private ValueAnimator.AnimatorUpdateListener mAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener(){
		@Override
		public void onAnimationUpdate( ValueAnimator valueAnimator ) {
			double originalChange = mDoubleTapDestinationScale - mHistoricalScale;
			double updatedChange = originalChange * (float) valueAnimator.getAnimatedValue();
			double currentScale = mHistoricalScale + updatedChange;
			setScale( currentScale );
			maintainScrollDuringScaleTween();
		}
	};

	private ValueAnimator.AnimatorListener mAnimatorListener = new ValueAnimator.AnimatorListener(){

		@Override
		public void onAnimationStart( Animator animator ) {
			saveHistoricalScale();
			isTweening = true;
			for ( ZoomPanListener listener : mZoomPanListeners) {
				listener.onZoomStart( scale );
				listener.onZoomPanEvent();
			}
		}

		@Override
		public void onAnimationEnd( Animator animator ) {
			isTweening = false;
			for ( ZoomPanListener listener : mZoomPanListeners) {
				listener.onZoomComplete( scale );
				listener.onZoomPanEvent();
			}
		}

		@Override
		public void onAnimationCancel( Animator animator ) {

		}

		@Override
		public void onAnimationRepeat( Animator animator ) {

		}
	};

	private boolean isTweening;
	private ValueAnimator mValueAnimator = ValueAnimator.ofFloat( 0, 1 );
	{
		mValueAnimator.addListener( mAnimatorListener );
		mValueAnimator.addUpdateListener( mAnimatorUpdateListener );
	}

	/**
	 * Constructor to use when creating a ZoomPanLayout from code.  Inflating from XML is not currently supported.
	 * @param context (Context) The Context the ZoomPanLayout is running in, through which it can access the current theme, resources, etc.
	 */
	public ZoomPanLayout( Context context ) {
		this(context, null);
	}

	public ZoomPanLayout( Context context, AttributeSet attrs ) {
		this(context, attrs, 0);
	}

	public ZoomPanLayout( Context context, AttributeSet attrs, int defStyleAttr ) {
		super(context, attrs, defStyleAttr);

		setWillNotDraw( false );

		mScrollActionHandler = new ScrollActionHandler( this );

		mScroller = new Scroller( context );

		mClip = new StaticLayout( context );
		super.addView( mClip, -1, new LayoutParams( -1, -1 ) );

		updateClip();
	}

	//------------------------------------------------------------------------------------
	// PUBLIC API
	//------------------------------------------------------------------------------------

	/**
	 * Determines whether the ZoomPanLayout should limit it's minimum scale to no less than what would be required to fill it's container
	 * @param shouldScaleToFit (boolean) True to limit minimum scale, false to allow arbitrary minimum scale (see {@link setScaleLimits})
	 */
	public void setShouldScaleToFit( boolean shouldScaleToFit ) {
		mShouldScaleToFit = shouldScaleToFit;
		calculateMinimumScaleToFit();
	}

	/**
	 * Set minimum and maximum scale values for this ZoomPanLayout.
	 * Note that if {@link mShouldScaleToFit} is set to true, the minimum value set here will be ignored
	 * Default values are 0 and 1.
	 * @param min
	 * @param max
	 */
	public void setScaleLimits( double min, double max ) {
		// if mShouldScaleToFit is set, don't allow overwrite
		if ( !mShouldScaleToFit) {
			mMinScale = min;
		}
		mMaxScale = max;
		setScale( scale );
	}

	/**
	 * Sets the size (width and height) of the ZoomPanLayout as it should be rendered at a scale of 1f (100%)
	 * @param wide width
	 * @param tall height
	 */
	public void setSize( int wide, int tall ) {
		mBaseWidth = wide;
		mBaseHeight = tall;
		mScaledWidth = (int) ( mBaseWidth * scale );
		mScaledHeight = (int) ( mBaseHeight * scale );
		updateClip();
	}

	/**
	 * Returns the base (un-scaled) width
	 * @return (int) base width
	 */
	public int getBaseWidth() {
		return mBaseWidth;
	}

	/**
	 * Returns the base (un-scaled) height
	 * @return (int) base height
	 */
	public int getBaseHeight() {
		return mBaseHeight;
	}

	/**
	 * Returns the scaled width
	 * @return (int) scaled width
	 */
	public int getScaledWidth() {
		return mScaledWidth;
	}

	/**
	 * Returns the scaled height
	 * @return (int) scaled height
	 */
	public int getScaledHeight() {
		return mScaledHeight;
	}

	/**
	 * Sets the scale (0-1) of the ZoomPanLayout
	 * @param scale (double) The new value of the ZoomPanLayout scale
	 */
	public void setScale( double d ) {
		d = Math.max( d, mMinScale );
		d = Math.min( d, mMaxScale );
		if ( scale != d ) {
			scale = d;
			mScaledWidth = (int) ( mBaseWidth * scale );
			mScaledHeight = (int) ( mBaseHeight * scale );
			updateClip();
			postInvalidate();
			for ( ZoomPanListener listener : mZoomPanListeners) {
				listener.onScaleChanged( scale );
				listener.onZoomPanEvent();
			}
		}
	}

	/**
	 * Requests a redraw
	 */
	public void redraw() {
		updateClip();
		postInvalidate();
	}

	/**
	 * Retrieves the current scale of the ZoomPanLayout
	 * @return (double) the current scale of the ZoomPanLayout
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * Returns whether the ZoomPanLayout is currently being flung
	 * @return (boolean) true if the ZoomPanLayout is currently flinging, false otherwise
	 */
	public boolean isFlinging() {
		return mIsBeingFlung;
	}

	/**
	 * Returns the single child of the ZoomPanLayout, a ViewGroup that serves as an intermediary container
	 * @return (View) The child view of the ZoomPanLayout that manages all contained views
	 */
	public View getClip() {
		return mClip;
	}

	/**
	 * Returns the Scroller instance used to manage dragging and flinging.
	 * @return (Scroller) The Scroller instance use to manage dragging and flinging.
	 */
	public Scroller getScroller(){
		return mScroller;
	}
	
	/**
	 * Returns the minimum distance required to start a drag operation, in pixels.
	 * @return (int) Pixel threshold required to start a drag.
	 */
	public int getDragStartThreshold(){
		return mDragStartThreshold;
	}
	
	/**
	 * Returns the minimum distance required to start a drag operation, in pixels.
	 * @param threshold (int) Pixel threshold required to start a drag.
	 */
	public void setDragStartThreshold( int threshold ){
		mDragStartThreshold = threshold;
	}
	
	/**
	 * Returns the minimum distance required to start a pinch operation, in pixels.
	 * @return (int) Pixel threshold required to start a pinch.
	 */
	public int getPinchStartThreshold(){
		return mPinchStartThreshold;
	}
	
	/**
	 * Returns the minimum distance required to start a pinch operation, in pixels.
	 * @param threshold (int) Pixel threshold required to start a pinch.
	 */
	public void setPinchStartThreshold( int threshold ){
		mPinchStartThreshold = threshold;
	}

	/**
	 * Adds a GestureListener to the ZoomPanLayout, which will receive gesture events
	 * @param listener (GestureListener) Listener to add
	 * @return (boolean) true when the listener set did not already contain the Listener, false otherwise 
	 */
	public boolean addGestureListener( GestureListener listener ) {
		return mGestureListeners.add( listener );
	}

	/**
	 * Removes a GestureListener from the ZoomPanLayout
	 * @param listener (GestureListener) Listener to remove
	 * @return (boolean) if the Listener was removed, false otherwise
	 */
	public boolean removeGestureListener( GestureListener listener ) {
		return mGestureListeners.remove( listener );
	}

	/**
	 * Adds a ZoomPanListener to the ZoomPanLayout, which will receive events relating to zoom and pan actions
	 * @param listener (ZoomPanListener) Listener to add
	 * @return (boolean) true when the listener set did not already contain the Listener, false otherwise 
	 */
	public boolean addZoomPanListener( ZoomPanListener listener ) {
		return mZoomPanListeners.add( listener );
	}

	/**
	 * Removes a ZoomPanListener from the ZoomPanLayout
	 * @param listener (ZoomPanListener) Listener to remove
	 * @return (boolean) if the Listener was removed, false otherwise
	 */
	public boolean removeZoomPanListener( ZoomPanListener listener ) {
		return mZoomPanListeners.remove( listener );
	}

	/**
	 * Scrolls the ZoomPanLayout to the x and y values specified by {@param point} Point
	 * @param point (Point) Point instance containing the destination x and y values
	 */
	public void scrollToPoint( Point point ) {
		constrainPoint( point );
		int ox = getScrollX();
		int oy = getScrollY();
		int nx = (int) point.x;
		int ny = (int) point.y;
		scrollTo( nx, ny );
		if ( ox != nx || oy != ny ) {
			for ( ZoomPanListener listener : mZoomPanListeners) {
				listener.onScrollChanged( nx, ny );
				listener.onZoomPanEvent();
			}
		}
	}

	/**
	 * Scrolls and centers the ZoomPanLayout to the x and y values specified by {@param point} Point
	 * @param point (Point) Point instance containing the destination x and y values
	 */
	public void scrollToAndCenter( Point point ) { // TODO:
		int x = (int) -( getWidth() * 0.5 );
		int y = (int) -( getHeight() * 0.5 );
		point.offset( x, y );
		scrollToPoint( point );
	}

	/**
	 * Scrolls the ZoomPanLayout to the x and y values specified by {@param point} Point using scrolling animation
	 * @param point (Point) Point instance containing the destination x and y values
	 */
	public void slideToPoint( Point point ) { // TODO:
		constrainPoint( point );
		int startX = getScrollX();
		int startY = getScrollY();
		int dx = point.x - startX;
		int dy = point.y - startY;
		mScroller.startScroll( startX, startY, dx, dy, SLIDE_DURATION );
		invalidate(); // we're posting invalidate in computeScroll, yet both are required
	}

	/**
	 * Scrolls and centers the ZoomPanLayout to the x and y values specified by {@param point} Point using scrolling animation
	 * @param point (Point) Point instance containing the destination x and y values
	 */
	public void slideToAndCenter( Point point ) { // TODO:
		int x = (int) -( getWidth() * 0.5 );
		int y = (int) -( getHeight() * 0.5 );
		point.offset( x, y );
		slideToPoint( point );
	}

	/**
	 * <i>This method is experimental</i>
	 * Scroll and scale to match passed Rect as closely as possible.
	 * The widget will attempt to frame the Rectangle, so that it's contained
	 * within the viewport, if possible.
	 * @param rect (Rect) rectangle to frame
	 */
	public void frameViewport( Rect rect ) {
		// position it
		scrollToPoint( new Point( rect.left, rect.top ) ); // TODO: center the axis that's smaller?
		// scale it
		double scaleX = getWidth() / (double) rect.width();
		double scaleY = getHeight() / (double) rect.height();
		double minimumScale = Math.min( scaleX, scaleY );
		smoothScaleTo( minimumScale, SLIDE_DURATION );

	}
	
	/**
	 * Set the scale of the ZoomPanLayout while maintaining the current center point
	 * @param scale (double) The new value of the ZoomPanLayout scale
	 */
	public void setScaleFromCenter( double s ) {

		s = Math.max( s, mMinScale );
		s = Math.min( s, mMaxScale );
		if ( s == scale )
			return;

		int centerOffsetX = (int) ( getWidth() * 0.5f );
		int centerOffsetY = (int) ( getHeight() * 0.5f );

		Point offset = new Point( centerOffsetX, centerOffsetY );
		Point scroll = new Point( getScrollX(), getScrollY() );
		scroll.offset( offset.x, offset.y );

		double deltaScale = s / getScale();

		int x = (int) ( scroll.x * deltaScale ) - offset.x;
		int y = (int) ( scroll.y * deltaScale ) - offset.y;
		Point destination = new Point( x, y );

		setScale( s );
		scrollToPoint( destination );

	}

	/**
	 * Adds a View to the intermediary ViewGroup that manages layout for the ZoomPanLayout.
	 * This View will be laid out at the width and height specified by {@setSize} at 0, 0
	 * All ViewGroup.addView signatures are routed through this signature, so the only parameters
	 * considered are child and index.
	 * @param child (View) The View to be added to the ZoomPanLayout view tree
	 * @param index (int) The position at which to add the child View
	 */
	@Override
	public void addView( View child, int index, LayoutParams params ) {
		LayoutParams lp = new LayoutParams( mScaledWidth, mScaledHeight );
		mClip.addView( child, index, lp );
	}

	@Override
	public void removeAllViews() {
		mClip.removeAllViews();
	}

	@Override
	public void removeViewAt( int index ) {
		mClip.removeViewAt( index );
	}

	@Override
	public void removeViews( int start, int count ) {
		mClip.removeViews( start, count );
	}

	/**
	 * Scales the ZoomPanLayout with animated progress
	 * @param destination (double) The final scale to animate to
	 * @param duration (int) The duration (in milliseconds) of the animation
	 */
	public void smoothScaleTo( double destination, int duration ) {
		if ( isTweening ) {
			return;
		}
		saveHistoricalScale();
		int x = (int) ( ( getWidth() * 0.5 ) + 0.5 );
		int y = (int) ( ( getHeight() * 0.5 ) + 0.5 );
		mDoubleTapStartOffset.set( x, y );
		mDoubleTapStartScroll.set( getScrollX(), getScrollY() );
		mDoubleTapStartScroll.offset( x, y );
		startSmoothScaleTo( destination, duration );
	}

    @Override
    public boolean canScrollHorizontally(int direction) {
        int currX = getScrollX();
        if (direction > 0) {
            return currX < getLimitX();
        } else if (direction < 0) {
            return currX > 0;
        }
        return false;
    }

    //------------------------------------------------------------------------------------
	// PRIVATE/PROTECTED
	//------------------------------------------------------------------------------------

	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
		measureChildren( widthMeasureSpec, heightMeasureSpec );
		int w = mClip.getMeasuredWidth();
		int h = mClip.getMeasuredHeight();
		w = Math.max( w, getSuggestedMinimumWidth() );
		h = Math.max( h, getSuggestedMinimumHeight() );
		w = resolveSize( w, widthMeasureSpec );
		h = resolveSize( h, heightMeasureSpec );
		setMeasuredDimension( w, h );
	}

	@Override
	protected void onLayout( boolean changed, int l, int t, int r, int b ) {
		mClip.layout( 0, 0, mClip.getMeasuredWidth(), mClip.getMeasuredHeight() );
		constrainScroll();
		if ( changed ) {
			calculateMinimumScaleToFit();
		}
	}

	private void calculateMinimumScaleToFit() {
		if (mShouldScaleToFit) {
			double minimumScaleX = getWidth() / (double) mBaseWidth;
			double minimumScaleY = getHeight() / (double) mBaseHeight;
			double recalculatedMinScale = Math.max( minimumScaleX, minimumScaleY );
			if ( recalculatedMinScale != mMinScale) {
				mMinScale = recalculatedMinScale;
				setScale( scale );
			}
		}
	}

	private void updateClip() {
		updateViewClip( mClip );
		for ( int i = 0; i < mClip.getChildCount(); i++ ) {
			View child = mClip.getChildAt( i );
			updateViewClip( child );
		}
		constrainScroll();
	}

	private void updateViewClip( View v ) {
		LayoutParams lp = v.getLayoutParams();
		lp.width = mScaledWidth;
		lp.height = mScaledHeight;
		v.setLayoutParams( lp );
	}

	@Override
	public void computeScroll() {
		if ( mScroller.computeScrollOffset() ) {
			Point destination = new Point( mScroller.getCurrX(), mScroller.getCurrY() );
			scrollToPoint( destination );
			dispatchScrollActionNotification();
			postInvalidate(); // should not be necessary but is...
		}
	}

	private void dispatchScrollActionNotification() {
		if ( mScrollActionHandler.hasMessages( 0 ) ) {
			mScrollActionHandler.removeMessages( 0 );
		}
		mScrollActionHandler.sendEmptyMessageDelayed( 0, 100 );
	}

	private void handleScrollerAction() {
		Point point = new Point();
		point.x = getScrollX();
		point.y = getScrollY();
		for ( GestureListener listener : mGestureListeners) {
			listener.onScrollComplete( point );
		}
		if (mIsBeingFlung) {
			mIsBeingFlung = false;
			for ( GestureListener listener : mGestureListeners) {
				listener.onFlingComplete( point );
			}
		}
	}

	private void constrainPoint( Point point ) {
		int x = point.x;
		int y = point.y;
		int mx = Math.max( 0, Math.min( x, getLimitX() ) );
		int my = Math.max( 0, Math.min( y, getLimitY() ) );
		if ( x != mx || y != my ) {
			point.set( mx, my );
		}
	}

	private void constrainScroll() { // TODO:
		Point currentScroll = new Point( getScrollX(), getScrollY() );
		Point limitScroll = new Point( currentScroll );
		constrainPoint( limitScroll );
		if ( !currentScroll.equals( limitScroll ) ) {
			scrollToPoint( limitScroll );
		}
	}

	private int getLimitX() {
		return mScaledWidth - getWidth();
	}

	private int getLimitY() {
		return mScaledHeight - getHeight();
	}

	private void saveHistoricalScale() {
		mHistoricalScale = scale;
	}

	private void savePinchHistory() {
		int x = (int) ( ( mFirstFinger.x + mSecondFinger.x ) * 0.5 );
		int y = (int) ( ( mFirstFinger.y + mSecondFinger.y ) * 0.5 );
		mPinchStartOffset.set( x, y );
		mPinchStartScroll.set( getScrollX(), getScrollY() );
		mPinchStartScroll.offset( x, y );
	}

	private void maintainScrollDuringPinchOperation() {
		double deltaScale = scale / mHistoricalScale;
		int x = (int) ( mPinchStartScroll.x * deltaScale ) - mPinchStartOffset.x;
		int y = (int) ( mPinchStartScroll.y * deltaScale ) - mPinchStartOffset.y;
		mDestinationScroll.set( x, y );
		scrollToPoint( mDestinationScroll );
	}

	private void saveDoubleTapHistory() {
		mDoubleTapStartOffset.set( mFirstFinger.x, mFirstFinger.y );
		mDoubleTapStartScroll.set( getScrollX(), getScrollY() );
		mDoubleTapStartScroll.offset( mDoubleTapStartOffset.x, mDoubleTapStartOffset.y );
	}

	private void maintainScrollDuringScaleTween() {
		double deltaScale = scale / mHistoricalScale;
		int x = (int) ( mDoubleTapStartScroll.x * deltaScale ) - mDoubleTapStartOffset.x;
		int y = (int) ( mDoubleTapStartScroll.y * deltaScale ) - mDoubleTapStartOffset.y;
		mDestinationScroll.set( x, y );
		scrollToPoint( mDestinationScroll );
	}

	private void saveHistoricalPinchDistance() {
		int dx = mFirstFinger.x - mSecondFinger.x;
		int dy = mFirstFinger.y - mSecondFinger.y;
		mPinchStartDistance = Math.sqrt( dx * dx + dy * dy );
	}

	private void setScaleFromPinch() {
		int dx = mFirstFinger.x - mSecondFinger.x;
		int dy = mFirstFinger.y - mSecondFinger.y;
		double pinchCurrentDistance = Math.sqrt( dx * dx + dy * dy );
		double currentScale = pinchCurrentDistance / mPinchStartDistance;
		currentScale = Math.max( currentScale, MINIMUM_PINCH_SCALE );
		currentScale = mHistoricalScale * currentScale;
		setScale( currentScale );
	}

	private void performDrag() {
		Point delta = new Point();
		if ( mSecondFingerIsDown && !mFirstFingerIsDown) {
			delta.set( mLastSecondFinger.x, mLastSecondFinger.y );
			delta.offset( -mSecondFinger.x, -mSecondFinger.y );
		} else {
			delta.set( mLastFirstFinger.x, mLastFirstFinger.y );
			delta.offset( -mFirstFinger.x, -mFirstFinger.y );
		}
		mScrollPosition.offset( delta.x, delta.y );
		scrollToPoint( mScrollPosition );
	}

	private boolean performFling() {
		if (mSecondFingerIsDown) {
			return false;
		}
		mVelocityTracker.computeCurrentVelocity( VELOCITY_UNITS );
		double xv = mVelocityTracker.getXVelocity();
		double yv = mVelocityTracker.getYVelocity();
		double totalVelocity = Math.abs( xv ) + Math.abs( yv );
		if ( totalVelocity > MINIMUM_VELOCITY ) {
			mScroller.fling( getScrollX(), getScrollY(), (int) -xv, (int) -yv, 0, getLimitX(), 0, getLimitY() );
			postInvalidate();
			return true;
		}
		return false;
	}

	// if the taps occurred within threshold, it's a double tap
	private boolean determineIfQualifiedDoubleTap() {
		long now = System.currentTimeMillis();
		long ellapsed = now - mLastTouchedAt;
		mLastTouchedAt = now;
		return ( ellapsed <= DOUBLE_TAP_TIME_THRESHOLD ) && ( Math.abs( mFirstFinger.x - mDoubleTapHistory.x ) <= SINGLE_TAP_DISTANCE_THRESHOLD )
				&& ( Math.abs( mFirstFinger.y - mDoubleTapHistory.y ) <= SINGLE_TAP_DISTANCE_THRESHOLD );

	}

	private void saveTapActionOrigination() {
		mSingleTapHistory.set( mFirstFinger.x, mFirstFinger.y );
	}

	private void saveDoubleTapOrigination() {
		mDoubleTapHistory.set( mFirstFinger.x, mFirstFinger.y );
	}
	
	private void saveFirstFingerDown() {
		mFirstFingerLastDown.set( mFirstFinger.x, mFirstFinger.y );
	}
	
	private void saveSecondFingerDown() {
		mSecondFingerLastDown.set( mSecondFinger.x, mSecondFinger.y );
	}

	private void setTapInterrupted( boolean v ) {
		mIsTapInterrupted = v;
	}

	// if the touch event has traveled past threshold since the finger first when down, it's not a tap
	private boolean determineIfQualifiedSingleTap() {
		return !mIsTapInterrupted && ( Math.abs( mFirstFinger.x - mSingleTapHistory.x ) <= SINGLE_TAP_DISTANCE_THRESHOLD )
				&& ( Math.abs( mFirstFinger.y - mSingleTapHistory.y ) <= SINGLE_TAP_DISTANCE_THRESHOLD );
	}
	
	private void startSmoothScaleTo( double destination, int duration ){
		if ( isTweening ) {
			return;
		}
		mDoubleTapDestinationScale = destination;
		mValueAnimator.setDuration( duration );
		mValueAnimator.start();
	}

	private void processEvent( MotionEvent event ) {

		// copy for history
		mLastFirstFinger.set( mFirstFinger.x, mFirstFinger.y );
		mLastSecondFinger.set( mSecondFinger.x, mSecondFinger.y );

		// set false for now
		mFirstFingerIsDown = false;
		mSecondFingerIsDown = false;

		// determine which finger is down and populate the appropriate points
		for ( int i = 0; i < event.getPointerCount(); i++ ) {
			int id = event.getPointerId( i );
			int x = (int) event.getX( i );
			int y = (int) event.getY( i );
			switch ( id ) {
			case 0:
				mFirstFingerIsDown = true;
				mFirstFinger.set( x, y );
				mActualPoint.set( x, y );
				break;
			case 1:
				mSecondFingerIsDown = true;
				mSecondFinger.set( x, y );
				mActualPoint.set( x, y );
				break;
			}
		}
		// record scroll position and adjust finger point to account for scroll offset
		mScrollPosition.set( getScrollX(), getScrollY() );
		mActualPoint.offset( mScrollPosition.x, mScrollPosition.y );

		// update mVelocityTracker for flinging
		if ( mVelocityTracker == null ) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement( event );

	}

	@Override
	public boolean onTouchEvent( MotionEvent event ) {
		// update positions
		processEvent( event );
		// get the type of action
		final int action = event.getAction() & MotionEvent.ACTION_MASK;
		// react based on nature of touch event
		switch ( action ) {
		// first finger goes down
		case MotionEvent.ACTION_DOWN:
			if ( !mScroller.isFinished() ) {
				mScroller.abortAnimation();
			}
			mIsBeingFlung = false;
			mIsDragging = false;
			setTapInterrupted( false );
			saveFirstFingerDown();
			saveTapActionOrigination();
			for ( GestureListener listener : mGestureListeners) {
				listener.onFingerDown( mActualPoint );
			}
			break;
		// second finger goes down
		case MotionEvent.ACTION_POINTER_DOWN:
			mIsPinching = false;
			saveSecondFingerDown();
			setTapInterrupted( true );
			for ( GestureListener listener : mGestureListeners) {
				listener.onFingerDown( mActualPoint );
			}
			break;
		// either finger moves
		case MotionEvent.ACTION_MOVE:
			// if both fingers are down, that means it's a pinch
			if ( mFirstFingerIsDown && mSecondFingerIsDown) {
				if ( !mIsPinching) {
					double firstFingerDistance = getDistance( mFirstFinger, mFirstFingerLastDown );
					double secondFingerDistance = getDistance( mSecondFinger, mSecondFingerLastDown );
					double distance = ( firstFingerDistance + secondFingerDistance ) * 0.5;
	                mIsPinching = distance >= mPinchStartThreshold;
	                // are we starting a pinch action?
					if (mIsPinching) {
						saveHistoricalPinchDistance();
						saveHistoricalScale();
						savePinchHistory();
						for ( GestureListener listener : mGestureListeners) {
							listener.onPinchStart( mPinchStartOffset );
						}
						for ( ZoomPanListener listener : mZoomPanListeners) {
							listener.onZoomStart( scale );
							listener.onZoomPanEvent();
						}
					}
				}
				if (mIsPinching) {
					setScaleFromPinch();
					maintainScrollDuringPinchOperation();
					for ( GestureListener listener : mGestureListeners) {
						listener.onPinch( mPinchStartOffset );
					}
				}				
				// otherwise it's a drag
			} else {
				if ( !mIsDragging) {
	                double distance = getDistance( mFirstFinger, mFirstFingerLastDown );
					mIsDragging = distance >= mDragStartThreshold;
				}
				if (mIsDragging) {
					performDrag();
					for ( GestureListener listener : mGestureListeners) {
						listener.onDrag( mActualPoint );
					}
				}
			}

			break;
		// first finger goes up
		case MotionEvent.ACTION_UP:
			if ( performFling() ) {
				mIsBeingFlung = true;
				Point startPoint = new Point( getScrollX(), getScrollY() );
				Point finalPoint = new Point( mScroller.getFinalX(), mScroller.getFinalY() );
				for ( GestureListener listener : mGestureListeners) {
					listener.onFling( startPoint, finalPoint );
				}
			}
			if ( mVelocityTracker != null ) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
			// could be a single tap...
			if ( determineIfQualifiedSingleTap() ) {
				for ( GestureListener listener : mGestureListeners) {
					listener.onTap( mActualPoint );
				}
			}
			// or a double tap
			if ( determineIfQualifiedDoubleTap() ) {
				mScroller.forceFinished( true );
				saveHistoricalScale();
				saveDoubleTapHistory();
				double destination;
				if ( scale >= mMaxScale) {
					destination = mMinScale;
				} else {
					destination = Math.min( mMaxScale, scale * 2 );
				}
				startSmoothScaleTo( destination, ZOOM_ANIMATION_DURATION );
				for ( GestureListener listener : mGestureListeners) {
					listener.onDoubleTap( mActualPoint );
				}
			}
			// either way it's a finger up event
			for ( GestureListener listener : mGestureListeners) {
				listener.onFingerUp( mActualPoint );
			}
			// save coordinates to measure against the next double tap
			saveDoubleTapOrigination();
			mIsDragging = false;
			mIsPinching = false;
			break;
		// second finger goes up
		case MotionEvent.ACTION_POINTER_UP:
			mIsPinching = false;
			setTapInterrupted( true );
			for ( GestureListener listener : mGestureListeners) {
				listener.onFingerUp( mActualPoint );
				listener.onPinchComplete( mPinchStartOffset );
			}
			for ( ZoomPanListener listener : mZoomPanListeners) {
				listener.onZoomComplete( scale );
				listener.onZoomPanEvent();
			}
			break;

		}

		return true;

	}
	
	// sugar to calculate distance between 2 Points, because android.graphics.Point is horrible
	private static double getDistance( Point p1, Point p2 ) {
		int x = p1.x - p2.x;
        int y = p1.y - p2.y;
        return Math.sqrt( x * x + y * y );
	}

	private static class ScrollActionHandler extends Handler {
		private final WeakReference<ZoomPanLayout> reference;

		public ScrollActionHandler( ZoomPanLayout zoomPanLayout ) {
			super();
			reference = new WeakReference<ZoomPanLayout>( zoomPanLayout );
		}

		@Override
		public void handleMessage( Message msg ) {
			ZoomPanLayout zoomPanLayout = reference.get();
			if ( zoomPanLayout != null ) {
				zoomPanLayout.handleScrollerAction();
			}
		}
	}

	//------------------------------------------------------------------------------------
	// Public static interfaces and classes
	//------------------------------------------------------------------------------------

	public static interface ZoomPanListener {
		public void onScaleChanged( double scale );
		public void onScrollChanged( int x, int y );
		public void onZoomStart( double scale );
		public void onZoomComplete( double scale );
		public void onZoomPanEvent();
	}

	public static interface GestureListener {
		public void onFingerDown( Point point );
		public void onScrollComplete( Point point );
		public void onFingerUp( Point point );
		public void onDrag( Point point );
		public void onDoubleTap( Point point );
		public void onTap( Point point );
		public void onPinch( Point point );
		public void onPinchStart( Point point );
		public void onPinchComplete( Point point );
		public void onFling( Point startPoint, Point finalPoint );
		public void onFlingComplete( Point point );
	}

}
