package com.qozix.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.Interpolator;

import java.lang.ref.WeakReference;

/**
 * @author Mike Dunn, 2/2/18.
 */

public class ScalingScrollView extends ScrollView implements
  GestureDetector.OnDoubleTapListener,
  ScaleGestureDetector.OnScaleGestureListener {

  public enum MinimumScaleMode {CONTAIN, COVER, NONE}

  private ScaleGestureDetector mScaleGestureDetector;
  private ScaleChangedListener mScaleChangedListener;

  private ZoomScrollAnimator mZoomScrollAnimator;

  private MinimumScaleMode mMinimumScaleMode = MinimumScaleMode.COVER;

  private float mScale = 1f;
  private float mMinScale = 0f;
  private float mMaxScale = 1f;
  private float mEffectiveMinScale = 0f;

  private boolean mWillHandleContentSize;
  private boolean mShouldVisuallyScaleContents;
  private boolean mShouldLoopScale = true;

  public ScalingScrollView(Context context) {
    this(context, null);
  }

  public ScalingScrollView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ScalingScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    mScaleGestureDetector = new ScaleGestureDetector(context, this);
    mZoomScrollAnimator = new ZoomScrollAnimator(this);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    boolean dispatched = super.dispatchTouchEvent(event);
    boolean scaled = mScaleGestureDetector.onTouchEvent(event);
    return dispatched || scaled;
  }

  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {
    super.onLayout(changed, l, t, r, b);
    calculateMinimumScaleToFit();
  }

  // getters and setters

  public ScaleChangedListener getScaleChangedListener() {
    return mScaleChangedListener;
  }

  public void setScaleChangedListener(ScaleChangedListener scaleChangedListener) {
    mScaleChangedListener = scaleChangedListener;
  }

  public float getScale() {
    return mScale;
  }

  public void setScale(float scale) {
    scale = getConstrainedDestinationScale(scale);
    if (mScale != scale) {
      float previous = mScale;
      mScale = scale;
      resetScrollPositionToWithinLimits();
      if (mShouldVisuallyScaleContents && hasContent()) {
        getChild().setPivotX(0);
        getChild().setPivotY(0);  // TODO: this is a hassle to prefab but would be more efficient
        getChild().setScaleX(mScale);
        getChild().setScaleY(mScale);
      }
      if (mScaleChangedListener != null) {
        mScaleChangedListener.onScaleChanged(this, mScale, previous);
      }
      invalidate();
    }
  }

  public ZoomScrollAnimator getAnimator() {
    return mZoomScrollAnimator;
  }

  // scale limits

  private void calculateMinimumScaleToFit() {
    float minimumScaleX = getWidth() / (float) getContentWidth();
    float minimumScaleY = getHeight() / (float) getContentHeight();
    float recalculatedMinScale = computeMinimumScaleForMode(minimumScaleX, minimumScaleY);
    if (recalculatedMinScale != mEffectiveMinScale) {
      mEffectiveMinScale = recalculatedMinScale;
      if (mScale < mEffectiveMinScale) {
        setScale(mEffectiveMinScale);
      }
    }
  }

  private float computeMinimumScaleForMode(float minimumScaleX, float minimumScaleY) {
    switch (mMinimumScaleMode) {
      case COVER:
        return Math.max(minimumScaleX, minimumScaleY);
      case CONTAIN:
        return Math.min(minimumScaleX, minimumScaleY);
    }
    return mMinScale;
  }

  public void setScaleLimits(float min, float max) {
    mMinScale = min;
    mMaxScale = max;
    setScale(mScale);
  }

  public void setMinimumScaleMode(MinimumScaleMode minimumScaleMode) {
    mMinimumScaleMode = minimumScaleMode;
    calculateMinimumScaleToFit();
  }

  @Override
  public int getContentWidth() {
    if (mWillHandleContentSize) {
      return super.getContentWidth();
    }
    return (int) (super.getContentWidth() * mScale);
  }

  @Override
  public int getContentHeight() {
    if (mWillHandleContentSize) {
      return super.getContentHeight();
    }
    return (int) (super.getContentHeight() * mScale);
  }

  private void resetScrollPositionToWithinLimits() {
    scrollTo(getScrollX(), getScrollY());
  }

  public void setShouldLoopScale(boolean shouldLoopScale) {
    mShouldLoopScale = shouldLoopScale;
  }

  // normally we constrain scroll to scaled "size", which is not appropriate if the child is resizing itself based on scale
  public void setWillHandleContentSize(boolean willHandleContentSize) {
    mWillHandleContentSize = willHandleContentSize;
  }

  public void setShouldVisuallyScaleContents(boolean shouldVisuallyScaleContents) {
    mShouldVisuallyScaleContents = shouldVisuallyScaleContents;
  }

  // doers

  private float getConstrainedDestinationScale(float scale) {
    scale = Math.max(scale, mEffectiveMinScale);
    scale = Math.min(scale, mMaxScale);
    return scale;
  }

  private int getOffsetScrollXFromScale(int offsetX, float destinationScale, float currentScale) {
    int scrollX = getScrollX() + offsetX;
    float deltaScale = destinationScale / currentScale;
    return (int) (scrollX * deltaScale) - offsetX;
  }

  private int getOffsetScrollYFromScale(int offsetY, float destinationScale, float currentScale) {
    int scrollY = getScrollY() + offsetY;
    float deltaScale = destinationScale / currentScale;
    return (int) (scrollY * deltaScale) - offsetY;
  }

  public void setScaleFromPosition(int offsetX, int offsetY, float scale) {
    scale = getConstrainedDestinationScale(scale);
    if (scale == mScale) {
      return;
    }
    int x = getOffsetScrollXFromScale(offsetX, scale, mScale);
    int y = getOffsetScrollYFromScale(offsetY, scale, mScale);

    setScale(scale);

    x = getConstrainedScrollX(x);
    y = getConstrainedScrollY(y);

    scrollTo(x, y);
  }

  public void smoothScaleFromFocalPoint(int focusX, int focusY, float scale) {
    scale = getConstrainedDestinationScale(scale);
    if (scale == mScale) {
      return;
    }
    int x = getOffsetScrollXFromScale(focusX, scale, mScale);
    int y = getOffsetScrollYFromScale(focusY, scale, mScale);
    getAnimator().animate(x, y, scale);
  }

  public void smoothScaleFromCenter(float scale) {
    smoothScaleFromFocalPoint(getWidth() / 2, getHeight() / 2, scale);
  }

  // interface methods

  @Override
  public boolean onSingleTapConfirmed(MotionEvent event) {
    return false;
  }

  @Override
  public boolean onDoubleTap(MotionEvent event) {
    float destination = (float) (Math.pow(2, Math.floor(Math.log(mScale * 2) / Math.log(2))));
    float effectiveDestination = mShouldLoopScale && mScale >= mMaxScale ? mMinScale : destination;
    destination = getConstrainedDestinationScale(effectiveDestination);
    smoothScaleFromFocalPoint((int) event.getX(), (int) event.getY(), destination);
    return true;
  }

  @Override
  public boolean onDoubleTapEvent(MotionEvent event) {
    return true;
  }

  @Override
  public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
    float currentScale = mScale * mScaleGestureDetector.getScaleFactor();
    setScaleFromPosition(
      (int) scaleGestureDetector.getFocusX(),
      (int) scaleGestureDetector.getFocusY(),
      currentScale);
    return true;
  }

  @Override
  public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
    return true;
  }

  @Override
  public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

  }

  public interface ScaleChangedListener {
    void onScaleChanged(ScalingScrollView scalingScrollView, float currentScale, float previousScale);
  }

  /**
   * @author Mike Dunn, 2/2/18.
   */

  private static class ZoomScrollAnimator extends ValueAnimator implements ValueAnimator.AnimatorUpdateListener {

    private WeakReference<ScalingScrollView> mScalingScrollViewWeakReference;
    private ScaleAndScrollState mStartState = new ScaleAndScrollState();
    private ScaleAndScrollState mEndState = new ScaleAndScrollState();
    private boolean mHasPendingZoomUpdates;
    private boolean mHasPendingScrollUpdates;

    public ZoomScrollAnimator(ScalingScrollView scalingScrollView) {
      super();
      addUpdateListener(this);
      setFloatValues(0f, 1f);
      setInterpolator(new QuinticInterpolator());
      mScalingScrollViewWeakReference = new WeakReference<>(scalingScrollView);
    }

    private boolean setupScrollAnimation(int x, int y) {
      ScalingScrollView scalingScrollView = mScalingScrollViewWeakReference.get();
      if (scalingScrollView != null) {
        mStartState.x = scalingScrollView.getScrollX();
        mStartState.y = scalingScrollView.getScrollY();
        mEndState.x = x;
        mEndState.y = y;
        return mStartState.x != mEndState.x || mStartState.y != mEndState.y;
      }
      return false;
    }

    private boolean setupZoomAnimation(float scale) {
      ScalingScrollView scalingScrollView = mScalingScrollViewWeakReference.get();
      if (scalingScrollView != null) {
        mStartState.scale = scalingScrollView.getScale();
        mEndState.scale = scale;
        return mStartState.scale != mEndState.scale;
      }
      return false;
    }

    public void animate(int x, int y, float scale) {
      ScalingScrollView scalingScrollView = mScalingScrollViewWeakReference.get();
      if (scalingScrollView != null) {
        mHasPendingZoomUpdates = setupZoomAnimation(scale);
        mHasPendingScrollUpdates = setupScrollAnimation(x, y);
        if (mHasPendingScrollUpdates || mHasPendingZoomUpdates) {
          start();
        }
      }
    }

    public void animateZoom(float scale) {
      ScalingScrollView scalingScrollView = mScalingScrollViewWeakReference.get();
      if (scalingScrollView != null) {
        mHasPendingZoomUpdates = setupZoomAnimation(scale);
        if (mHasPendingZoomUpdates) {
          start();
        }
      }
    }

    public void animateScroll(int x, int y) {
      ScalingScrollView scalingScrollView = mScalingScrollViewWeakReference.get();
      if (scalingScrollView != null) {
        mHasPendingScrollUpdates = setupScrollAnimation(x, y);
        if (mHasPendingScrollUpdates) {
          start();
        }
      }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
      ScalingScrollView scalingScrollView = mScalingScrollViewWeakReference.get();
      if (scalingScrollView != null) {
        float progress = (float) animation.getAnimatedValue();
        if (mHasPendingZoomUpdates) {
          float scale = mStartState.scale + (mEndState.scale - mStartState.scale) * progress;
          scalingScrollView.setScale(scale);
        }
        if (mHasPendingScrollUpdates) {
          int x = (int) (mStartState.x + (mEndState.x - mStartState.x) * progress);
          int y = (int) (mStartState.y + (mEndState.y - mStartState.y) * progress);
          scalingScrollView.scrollTo(x, y);
        }
      }
    }

    private static class ScaleAndScrollState {
      public int x;
      public int y;
      public float scale;
    }

    // https://android.googlesource.com/platform/frameworks/support/+/master/v7/recyclerview/src/main/java/android/support/v7/widget/RecyclerView.java#514
    private static class QuinticInterpolator implements Interpolator {
      @Override
      public float getInterpolation(float t) {
        t -= 1.0f;
        return t * t * t * t * t + 1.0f;
      }
    }
  }
}
