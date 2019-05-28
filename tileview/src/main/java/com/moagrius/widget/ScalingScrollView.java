package com.moagrius.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.Interpolator;

import com.moagrius.view.PointerDownGestureDetector;

import java.lang.ref.WeakReference;

/**
 * @author Mike Dunn, 2/2/18.
 */

public class ScalingScrollView extends ScrollView implements
  PointerDownGestureDetector.OnPointerDownListener,
  ScaleGestureDetector.OnScaleGestureListener {

  private static final int SCROLL_GESTURE_MAX_POINTER_COUNT = 1;

  public enum MinimumScaleMode {CONTAIN, COVER, NONE}

  private PointerDownGestureDetector mPointerDownGestureDetector;
  private ScaleGestureDetector mScaleGestureDetector;
  private ScaleChangedListener mScaleChangedListener;

  private ZoomScrollAnimator mZoomScrollAnimator;

  private MinimumScaleMode mMinimumScaleMode = MinimumScaleMode.COVER;

  private ScrollScaleState mSavedState;

  private float mScale = 1f;
  private float mMinScale = 0f;
  private float mMaxScale = 1f;
  private float mEffectiveMinScale = 0f;

  private boolean mIsScaling;
  private boolean mHasSingleFingerDown;
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
    mPointerDownGestureDetector = new PointerDownGestureDetector(context, this);
    mScaleGestureDetector = new ScaleGestureDetector(context, this);
    mZoomScrollAnimator = new ZoomScrollAnimator(this);
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    ScrollScaleState sss = (ScrollScaleState) state;
    super.onRestoreInstanceState(sss.getSuperState());
    setScale(sss.scale);
  }

  @Override
  protected Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    ScrollScaleState sss = new ScrollScaleState(superState);
    sss.scale = mScale;
    return sss;
  }

  private void setIsScaling(boolean isScaling) {
    mIsScaling = isScaling;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    mScaleGestureDetector.onTouchEvent(event);
    if (mIsScaling) {
      return true;
    }
    mIsScaling = mPointerDownGestureDetector.onTouchEvent(event);
    if (mIsScaling) {
      return true;
    }
    if (mHasSingleFingerDown) {
      return super.onTouchEvent(event);
    }
    return false;
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

  protected void calculateMinimumScaleToFit() {
    float effectiveWidth = getContentWidth() / mScale;
    float effectiveHeight = getContentHeight() / mScale;
    float minimumScaleX = getWidth() / effectiveWidth;
    float minimumScaleY = getHeight() / effectiveHeight;
    float recalculatedMinScale = computeMinimumScaleForMode(minimumScaleX, minimumScaleY);
    if (recalculatedMinScale != mEffectiveMinScale) {
      mEffectiveMinScale = recalculatedMinScale;
      if (mScale < mEffectiveMinScale) {
        setScale(mEffectiveMinScale);
      }
    }
  }

  protected float computeMinimumScaleForMode(float minimumScaleX, float minimumScaleY) {
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

  public void setMinimumScale(float min) {
    mMinScale = min;
    setScale(mScale);
  }

  public void setMaximumScale(float max) {
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

  public void resetScrollPositionToWithinLimits() {
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

  protected float getConstrainedDestinationScale(float scale) {
    scale = Math.max(scale, mEffectiveMinScale);
    scale = Math.min(scale, mMaxScale);
    return scale;
  }

  protected int getOffsetScrollXFromScale(int offsetX, float destinationScale, float currentScale) {
    int scrollX = getScrollX() + offsetX;
    float deltaScale = destinationScale / currentScale;
    return (int) (scrollX * deltaScale) - offsetX;
  }

  protected int getOffsetScrollYFromScale(int offsetY, float destinationScale, float currentScale) {
    int scrollY = getScrollY() + offsetY;
    float deltaScale = destinationScale / currentScale;
    return (int) (scrollY * deltaScale) - offsetY;
  }

  public void setScaleFromPosition(int offsetX, int offsetY, float scale) {
    scale = getConstrainedDestinationScale(scale);
    if (scale == mScale) {
      return;
    }

    float previous = mScale;

    setScale(scale);

    int x = getOffsetScrollXFromScale(offsetX, mScale, previous);
    int y = getOffsetScrollYFromScale(offsetY, mScale, previous);

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

  public void smoothScaleAndScrollTo(int x, int y, float scale) {
    getAnimator().animate(x, y, scale);
  }

  // interface methods

  @Override
  public void onDoubleTap(MotionEvent event) {
    Log.d("double-tap", "double tap");
    float destination = (float) (Math.pow(2, Math.floor(Math.log(mScale * 2) / Math.log(2))));
    float effectiveDestination = mShouldLoopScale && mScale >= mMaxScale ? mMinScale : destination;
    destination = getConstrainedDestinationScale(effectiveDestination);
    smoothScaleFromFocalPoint((int) event.getX(), (int) event.getY(), destination);
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
    mIsScaling = true;
    return true;
  }

  @Override
  public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
    mIsScaling = false;
  }

  @Override
  public void onPointerCounterChange(int pointerCount) {
    mHasSingleFingerDown = (pointerCount == SCROLL_GESTURE_MAX_POINTER_COUNT);
  }

  @Override
  public void onDraw( Canvas canvas ) {
    super.onDraw( canvas );
    if (mShouldVisuallyScaleContents) {
      canvas.scale(mScale, mScale);
    }
  }

  public interface ScaleChangedListener {
    void onScaleChanged(ScalingScrollView scalingScrollView, float currentScale, float previousScale);
  }

  protected static class ScrollScaleState extends SavedState {
    public float scale = 1f;

    public ScrollScaleState(Parcelable superState) {
      super(superState);
    }

    public ScrollScaleState(Parcel source) {
      super(source);
      scale = source.readFloat();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      super.writeToParcel(dest, flags);
      dest.writeFloat(scale);
    }

    @Override
    public String toString() {
      return "ScalingScrollView.ScrollScaleState{" + Integer.toHexString(System.identityHashCode(this)) + " scrollPositionY=" + scrollPositionY + ", scrollPositionX=" + scrollPositionX + ", scale=" + scale + "}";
    }

    public static final Creator<ScrollScaleState> CREATOR = new Creator<ScrollScaleState>() {
      public ScrollScaleState createFromParcel(Parcel in) {
        return new ScrollScaleState(in);
      }

      public ScrollScaleState[] newArray(int size) {
        return new ScrollScaleState[size];
      }
    };
  }

  /**
   * @author Mike Dunn, 2/2/18.
   */

  private static class ZoomScrollAnimator extends ValueAnimator implements
      ValueAnimator.AnimatorUpdateListener,
      ValueAnimator.AnimatorListener {

    private WeakReference<ScalingScrollView> mScalingScrollViewWeakReference;
    private ScaleAndScrollState mStartState = new ScaleAndScrollState();
    private ScaleAndScrollState mEndState = new ScaleAndScrollState();
    private boolean mHasPendingZoomUpdates;
    private boolean mHasPendingScrollUpdates;

    public ZoomScrollAnimator(ScalingScrollView scalingScrollView) {
      super();
      addUpdateListener(this);
      addListener(this);
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
          scalingScrollView.setIsScaling(true);
        }
        if (mHasPendingScrollUpdates) {
          int x = (int) (mStartState.x + (mEndState.x - mStartState.x) * progress);
          int y = (int) (mStartState.y + (mEndState.y - mStartState.y) * progress);
          scalingScrollView.scrollTo(x, y);
        }
      }
    }

    @Override
    public void onAnimationStart(Animator animator) {

    }

    @Override
    public void onAnimationEnd(Animator animator) {
      ScalingScrollView scalingScrollView = mScalingScrollViewWeakReference.get();
      if (scalingScrollView != null) {
        scalingScrollView.setIsScaling(false);
      }
    }

    @Override
    public void onAnimationCancel(Animator animator) {
      ScalingScrollView scalingScrollView = mScalingScrollViewWeakReference.get();
      if (scalingScrollView != null) {
        scalingScrollView.setIsScaling(false);
      }
    }

    @Override
    public void onAnimationRepeat(Animator animator) {

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
