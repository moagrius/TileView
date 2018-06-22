package com.qozix.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.FocusFinder;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.Scroller;

import com.qozix.tileview.R;


/**
 * This is a 2D scroller modified from ScrollView and HorizontalScrollView,
 * taken from the KitKat release (API 16)
 * At the time of this writing, KitKat and later accounted for more than 95% of devices according to https://developer.android.com/about/dashboards/
 * https://android.googlesource.com/platform/frameworks/base/+/kitkat-release/core/java/android/widget/ScrollView.java
 * https://android.googlesource.com/platform/frameworks/base/+/kitkat-release/core/java/android/widget/HorizontalScrollView.java
 *
 * Some minor changes were also made, informed by the most modern version at the time of this writing:
 * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/widget/ScrollView.java
 * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/widget/HorizontalScrollView.java
 * and similar classes, like:
 * https://android.googlesource.com/platform/frameworks/support/+/master/v7/recyclerview/src/main/java/android/support/v7/widget/RecyclerView.java
 * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/view/GestureDetector.java
 *
 * I've modified from the source for a few reasons:
 * 1. Anything that was required for functionality on both axes.
 * 2. Inaccessibility (package-private, internal, etc).
 * 3. There's very little left around child focus, as a 2D scroll view is likely to be a form container.
 * 4. Fading edge logic has been removed.
 * 5. Certain accessibility functions have been removed (e.g., does "scroll forward" mean down, or right?)
 * 6. Using a Scroller rather than an OverScroller; over-scroll seems less helpful for "panning" views than a list-type view.
 *
 * Mike Dunn
 * June 2018
 */

public class ScrollView extends FrameLayout {

  private static final int ANIMATED_SCROLL_GAP = 250;
  private static final int INVALID_POINTER = -1;

  private static final int DIRECTION_BACKWARD = -1;
  private static final int DIRECTION_FORWARD = 1;

  private static final String ADD_VIEW_ERROR_MESSAGE = "ScrollView can host only one direct child";

  private long mLastScroll;
  private final Rect mTempRect = new Rect();
  private Scroller mScroller;
  private int mLastMotionY;
  private int mLastMotionX;
  private boolean mIsLayoutDirty = true;
  private View mChildToScrollTo = null;
  private boolean mIsBeingDragged = false;
  private VelocityTracker mVelocityTracker;
  private boolean mFillViewport;
  private boolean mSmoothScrollingEnabled = true;
  private int mTouchSlop;
  private int mMinimumVelocity;
  private int mMaximumVelocity;
  private int mActivePointerId = INVALID_POINTER;
  private SavedState mSavedState;

  public ScrollView(Context context) {
    this(context, null);
  }

  public ScrollView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ScrollView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initScrollView();
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ScrollView, defStyle, 0);
    setFillViewport(a.getBoolean(R.styleable.ScrollView_fillViewport, false));
    a.recycle();
  }

  @Override
  public boolean shouldDelayChildPressedState() {
    return true;
  }

  private void initScrollView() {
    setFocusable(true);
    setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
    setWillNotDraw(false);
    final ViewConfiguration configuration = ViewConfiguration.get(getContext());
    mTouchSlop = configuration.getScaledTouchSlop();
    mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
    mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    mScroller = new Scroller(getContext());
  }

  public boolean isFillViewport() {
    return mFillViewport;
  }

  public void setFillViewport(boolean fillViewport) {
    if (fillViewport != mFillViewport) {
      mFillViewport = fillViewport;
      requestLayout();
    }
  }

  protected boolean hasContent() {
    return getChildCount() > 0;
  }

  protected View getChild() {
    if (hasContent()) {
      return getChildAt(0);
    }
    return null;
  }

  // SCROLLING

  public int getContentWidth() {
    if (hasContent()) {
      return getChild().getMeasuredWidth();
    }
    return 0;
  }

  public int getContentHeight() {
    if (hasContent()) {
      return getChild().getMeasuredHeight();
    }
    return 0;
  }

  protected int getConstrainedScrollX(int x) {
    return Math.max(getScrollMinX(), Math.min(x, getHorizontalScrollRange()));
  }

  protected int getConstrainedScrollY(int y) {
    return Math.max(getScrollMinY(), Math.min(y, getVerticalScrollRange()));
  }

  private int getVerticalScrollRange() {
    if (!hasContent()) {
      return 0;
    }
    return Math.max(0, getContentHeight() - (getHeight() - getPaddingBottom() - getPaddingTop()));
  }

  private int getHorizontalScrollRange() {
    if (!hasContent()) {
      return 0;
    }
    return Math.max(0, getContentWidth() - (getWidth() - getPaddingLeft() - getPaddingRight()));
  }

  protected int getContentRight() {
    if (hasContent()) {
      return getChild().getLeft() + getContentWidth();
    }
    return 0;
  }

  protected int getContentBottom() {
    if (hasContent()) {
      return getChild().getTop() + getContentHeight();
    }
    return 0;
  }

  @Override
  protected int computeHorizontalScrollRange() {
    if (!hasContent()) {
      return getWidth() - getPaddingLeft() - getPaddingRight();
    }
    return getContentRight();
  }

  @Override
  protected int computeVerticalScrollRange() {
    if (!hasContent()) {
      return getHeight() - getPaddingBottom() - getPaddingTop();
    }
    return getContentBottom();
  }

  @Override
  protected int computeHorizontalScrollOffset() {
    return Math.max(0, super.computeHorizontalScrollOffset());
  }

  @Override
  protected int computeVerticalScrollOffset() {
    return Math.max(0, super.computeVerticalScrollOffset());
  }

  protected int getScrollMinX() {
    return 0;
  }

  protected int getScrollMinY() {
    return 0;
  }

  @Override
  public boolean canScrollHorizontally(int direction) {
    int position = getScrollX();
    return direction > 0 ? position < getHorizontalScrollRange() : direction < 0 && position > 0;
  }

  @Override
  public boolean canScrollVertically(int direction) {
    int position = getScrollY();
    return direction > 0 ? position < getVerticalScrollRange() : direction < 0 && position > 0;
  }

  public boolean canScroll(int direction) {
    return canScrollVertically(direction) || canScrollHorizontally(direction);
  }

  public boolean canScroll() {
    return canScroll(DIRECTION_FORWARD) || canScroll(DIRECTION_BACKWARD);
  }

  public boolean isSmoothScrollingEnabled() {
    return mSmoothScrollingEnabled;
  }

  public void setSmoothScrollingEnabled(boolean smoothScrollingEnabled) {
    mSmoothScrollingEnabled = smoothScrollingEnabled;
  }

  public final void smoothScrollBy(int dx, int dy) {
    if (!hasContent()) {
      return;
    }
    long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
    if (duration > ANIMATED_SCROLL_GAP) {
      final int width = getWidth() - getPaddingRight() - getPaddingLeft();
      final int right = getChildAt(0).getWidth();
      final int maxX = Math.max(0, right - width);
      final int scrollX = getScrollX();
      dx = Math.max(0, Math.min(scrollX + dx, maxX)) - scrollX;
      final int height = getHeight() - getPaddingBottom() - getPaddingTop();
      final int bottom = getChild().getHeight();
      final int maxY = Math.max(0, bottom - height);
      final int scrollY = getScrollY();
      dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;
      mScroller.startScroll(scrollX, scrollY, dx, dy);
      postInvalidateOnAnimation();
    } else {
      if (!mScroller.isFinished()) {
        mScroller.abortAnimation();
      }
      scrollBy(dx, dy);
    }
    mLastScroll = AnimationUtils.currentAnimationTimeMillis();
  }

  public final void smoothScrollTo(int x, int y) {
    smoothScrollBy(x - getScrollX(), y - getScrollY());
  }

  private void performScrollBy(int x, int y) {
    if (mSmoothScrollingEnabled) {
      smoothScrollBy(x, y);
    } else {
      scrollBy(x, y);
    }
  }

  @Override
  public void scrollTo(int x, int y) {
    if (hasContent()) {
      x = getConstrainedScrollX(x);
      y = getConstrainedScrollY(y);
      if (x != getScrollX() || y != getScrollY()) {
        super.scrollTo(x, y);
      }
    }
  }

  @Override
  public void scrollBy(int x, int y) {
    scrollTo(getScrollX() + x, getScrollY() + y);
  }

  @Override
  public void computeScroll() {
    if (mScroller.computeScrollOffset()) {
      int x = mScroller.getCurrX();
      int y = mScroller.getCurrY();
      scrollTo(x, y);
      if (!awakenScrollBars()) {
        postInvalidateOnAnimation();
      }
    }
  }

  private void initOrResetVelocityTracker() {
    if (mVelocityTracker == null) {
      mVelocityTracker = VelocityTracker.obtain();
    } else {
      mVelocityTracker.clear();
    }
  }

  private void initVelocityTrackerIfNotExists() {
    if (mVelocityTracker == null) {
      mVelocityTracker = VelocityTracker.obtain();
    }
  }

  private void recycleVelocityTracker() {
    if (mVelocityTracker != null) {
      mVelocityTracker.recycle();
      mVelocityTracker = null;
    }
  }

  @Override
  public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    if (disallowIntercept) {
      recycleVelocityTracker();
    }
    super.requestDisallowInterceptTouchEvent(disallowIntercept);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    final int action = event.getAction();
    if (action == MotionEvent.ACTION_MOVE && mIsBeingDragged) {
      return true;
    }
    if (!canScroll()) {
      return false;
    }
    switch (action & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_MOVE: {
        final int activePointerId = mActivePointerId;
        if (activePointerId == INVALID_POINTER) {
          break;
        }
        final int pointerIndex = event.findPointerIndex(activePointerId);
        if (pointerIndex == -1) {
          break;
        }
        final int x = (int) event.getX(pointerIndex);
        final int y = (int) event.getY(pointerIndex);
        final int xDiff = Math.abs(x - mLastMotionX);
        final int yDiff = Math.abs(y - mLastMotionY);
        if (yDiff > mTouchSlop || xDiff > mTouchSlop) {
          mIsBeingDragged = true;
          mLastMotionY = y;
          mLastMotionX = x;
          initVelocityTrackerIfNotExists();
          mVelocityTracker.addMovement(event);
          final ViewParent parent = getParent();
          if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
          }
        }
        break;
      }
      case MotionEvent.ACTION_DOWN: {
        final int y = (int) event.getY();
        final int x = (int) event.getX();
        if (!inChild(x, y)) {
          mIsBeingDragged = false;
          recycleVelocityTracker();
          break;
        }
        mLastMotionY = y;
        mLastMotionX = x;
        mActivePointerId = event.getPointerId(0);
        initOrResetVelocityTracker();
        mVelocityTracker.addMovement(event);
        mIsBeingDragged = !mScroller.isFinished();
        break;
      }
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        mIsBeingDragged = false;
        mActivePointerId = INVALID_POINTER;
        recycleVelocityTracker();
        break;
      case MotionEvent.ACTION_POINTER_UP:
        onSecondaryPointerUp(event);
        break;
    }
    return mIsBeingDragged;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    initVelocityTrackerIfNotExists();
    mVelocityTracker.addMovement(event);
    final int action = event.getAction();
    switch (action & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN: {
        if (!hasContent()) {
          return false;
        }
        if (mIsBeingDragged = !mScroller.isFinished()) {
          final ViewParent parent = getParent();
          if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
          }
        }
        if (!mScroller.isFinished()) {
          mScroller.abortAnimation();
        }
        mLastMotionY = (int) event.getY();
        mLastMotionX = (int) event.getX();
        mActivePointerId = event.getPointerId(0);
        break;
      }
      case MotionEvent.ACTION_MOVE:
        final int activePointerIndex = event.findPointerIndex(mActivePointerId);
        if (activePointerIndex == -1) {
          break;
        }
        final int y = (int) event.getY(activePointerIndex);
        final int x = (int) event.getX(activePointerIndex);
        int deltaY = mLastMotionY - y;
        int deltaX = mLastMotionX - x;
        if (!mIsBeingDragged && (Math.abs(deltaY) > mTouchSlop || Math.abs(deltaX) > mTouchSlop)) {
          final ViewParent parent = getParent();
          if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
          }
          mIsBeingDragged = true;
          deltaX += mTouchSlop * (deltaX < 0 ? -1 : 1);
          deltaY += mTouchSlop * (deltaY < 0 ? -1 : 1);
        }
        if (mIsBeingDragged) {
          mLastMotionY = y;
          mLastMotionX = x;
          scrollBy(deltaX, deltaY);
        }
        break;
      case MotionEvent.ACTION_UP:
        if (mIsBeingDragged) {
          mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
          int velocityX = (int) mVelocityTracker.getXVelocity(mActivePointerId);
          int velocityY = (int) mVelocityTracker.getYVelocity(mActivePointerId);
          if (hasContent()) {
            if (Math.abs(velocityX) > mMinimumVelocity || Math.abs(velocityY) > mMinimumVelocity) {
              mScroller.fling(getScrollX(), getScrollY(), -velocityX, -velocityY, 0, getHorizontalScrollRange(), 0, getVerticalScrollRange());
              postInvalidateOnAnimation();
            }
          }
          mActivePointerId = INVALID_POINTER;
          endDrag();
        }
        break;
      case MotionEvent.ACTION_CANCEL:
        if (mIsBeingDragged && hasContent()) {
          mActivePointerId = INVALID_POINTER;
          endDrag();
        }
        break;
      case MotionEvent.ACTION_POINTER_DOWN: {
        final int index = event.getActionIndex();
        mLastMotionY = (int) event.getY(index);
        mLastMotionX = (int) event.getX(index);
        mActivePointerId = event.getPointerId(index);
        break;
      }
      case MotionEvent.ACTION_POINTER_UP:
        onSecondaryPointerUp(event);
        mLastMotionY = (int) event.getY(event.findPointerIndex(mActivePointerId));
        mLastMotionX = (int) event.getX(event.findPointerIndex(mActivePointerId));
        break;
    }
    return true;
  }

  private void onSecondaryPointerUp(MotionEvent ev) {
    final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
    final int pointerId = ev.getPointerId(pointerIndex);
    if (pointerId == mActivePointerId) {
      final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
      mLastMotionY = (int) ev.getY(newPointerIndex);
      mActivePointerId = ev.getPointerId(newPointerIndex);
      if (mVelocityTracker != null) {
        mVelocityTracker.clear();
      }
    }
  }

  private void endDrag() {
    mIsBeingDragged = false;
    recycleVelocityTracker();
  }

  private void scrollToChild(View child) {
    child.getDrawingRect(mTempRect);
    offsetDescendantRectToMyCoords(child, mTempRect);
    int deltaX = computeScrollXDeltaToGetChildRectOnScreen(mTempRect);
    int deltaY = computeScrollYDeltaToGetChildRectOnScreen(mTempRect);
    if (deltaY != 0 || deltaX != 0) {
      scrollBy(deltaX, deltaY);
    }
  }

  private boolean scrollToChildRect(Rect rect, boolean immediate) {
    final int deltaX = computeScrollXDeltaToGetChildRectOnScreen(rect);
    final int deltaY = computeScrollYDeltaToGetChildRectOnScreen(rect);
    final boolean scroll = deltaY != 0 || deltaX != 0;
    if (scroll) {
      if (immediate) {
        scrollBy(deltaX, deltaY);
      } else {
        smoothScrollBy(deltaX, deltaY);
      }
    }
    return scroll;
  }

  protected int computeScrollYDeltaToGetChildRectOnScreen(Rect rect) {
    if (!hasContent()) {
      return 0;
    }
    int height = getHeight();
    int screenTop = getScrollY();
    int screenBottom = screenTop + height;
    int scrollYDelta = 0;
    if (rect.bottom > screenBottom && rect.top > screenTop) {
      if (rect.height() > height) {
        scrollYDelta += (rect.top - screenTop);
      } else {
        scrollYDelta += (rect.bottom - screenBottom);
      }
      int bottom = getChild().getBottom();
      int distanceToBottom = bottom - screenBottom;
      scrollYDelta = Math.min(scrollYDelta, distanceToBottom);
    } else if (rect.top < screenTop && rect.bottom < screenBottom) {
      if (rect.height() > height) {
        scrollYDelta -= (screenBottom - rect.bottom);
      } else {
        scrollYDelta -= (screenTop - rect.top);
      }
      scrollYDelta = Math.max(scrollYDelta, -getScrollY());
    }
    return scrollYDelta;
  }

  protected int computeScrollXDeltaToGetChildRectOnScreen(Rect rect) {
    if (!hasContent()) {
      return 0;
    }
    int width = getWidth();
    int screenLeft = getScrollX();
    int screenRight = screenLeft + width;
    int scrollXDelta = 0;
    if (rect.right > screenRight && rect.left > screenLeft) {
      if (rect.width() > width) {
        scrollXDelta += (rect.left - screenLeft);
      } else {
        scrollXDelta += (rect.right - screenRight);
      }
      int right = getChild().getRight();
      int distanceToRight = right - screenRight;
      scrollXDelta = Math.min(scrollXDelta, distanceToRight);
    } else if (rect.left < screenLeft && rect.right < screenRight) {
      if (rect.width() > width) {
        scrollXDelta -= (screenRight - rect.right);
      } else {
        scrollXDelta -= (screenLeft - rect.left);
      }
      scrollXDelta = Math.max(scrollXDelta, -getScrollX());
    }
    return scrollXDelta;
  }

  // VIEW HIERARCHY, LAYOUT & MEASURE

  private void assertSingleChild() {
    if (getChildCount() > 0) {
      throw new IllegalStateException(ADD_VIEW_ERROR_MESSAGE);
    }
  }

  @Override
  public void addView(View child) {
    assertSingleChild();
    super.addView(child);
  }

  @Override
  public void addView(View child, int index) {
    assertSingleChild();
    super.addView(child, index);
  }

  @Override
  public void addView(View child, ViewGroup.LayoutParams params) {
    assertSingleChild();
    super.addView(child, params);
  }

  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    assertSingleChild();
    super.addView(child, index, params);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if (!mFillViewport) {
      return;
    }
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    if (heightMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.UNSPECIFIED) {
      return;
    }
    if (hasContent()) {
      final View child = getChild();
      int height = getMeasuredHeight();
      int width = getMeasuredWidth();
      final FrameLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
      if (child.getMeasuredHeight() < height || child.getMeasuredWidth() < width) {
        int childWidthMeasureSpec;
        int childHeightMeasureSpec;
        if (child.getMeasuredHeight() < height) {
          height -= getPaddingTop();
          height -= getPaddingBottom();
          childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        } else {
          childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom(), lp.height);
        }
        if (child.getMeasuredWidth() < width) {
          width -= getPaddingLeft();
          width -= getPaddingRight();
          childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        } else {
          childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight(), lp.width);
        }
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
      }
    }
  }

  @Override
  protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
    ViewGroup.LayoutParams lp = child.getLayoutParams();
    final int childWidthMeasureSpec = getScrollViewChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft() + getPaddingRight(), lp.width);
    final int childHeightMeasureSpec = getScrollViewChildMeasureSpec(parentHeightMeasureSpec, getPaddingTop() + getPaddingBottom(), lp.height);
    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
  }

  @Override
  protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
    final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
    final int childWidthMeasureSpec = getScrollViewChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin + widthUsed, lp.width);
    final int childHeightMeasureSpec = getScrollViewChildMeasureSpec(parentHeightMeasureSpec, getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin + widthUsed, lp.height);
    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
  }

  public static int getScrollViewChildMeasureSpec(int spec, int padding, int childDimension) {
    int specMode = MeasureSpec.getMode(spec);
    int specSize = MeasureSpec.getSize(spec);

    int size = Math.max(0, specSize - padding);

    int resultSize = 0;
    int resultMode = 0;

    switch (specMode) {
      case MeasureSpec.EXACTLY:
        if (childDimension >= 0) {
          resultSize = childDimension;
          resultMode = MeasureSpec.EXACTLY;
        } else if (childDimension == LayoutParams.MATCH_PARENT) {
          resultSize = size;
          resultMode = MeasureSpec.EXACTLY;
        } else if (childDimension == LayoutParams.WRAP_CONTENT) {
          resultSize = size;
          resultMode = MeasureSpec.UNSPECIFIED;
        }
        break;

      case MeasureSpec.AT_MOST:
        if (childDimension >= 0) {
          resultSize = childDimension;
          resultMode = MeasureSpec.EXACTLY;
        } else if (childDimension == LayoutParams.MATCH_PARENT) {
          resultSize = size;
          resultMode = MeasureSpec.AT_MOST;
        } else if (childDimension == LayoutParams.WRAP_CONTENT) {
          resultSize = size;
          resultMode = MeasureSpec.UNSPECIFIED;
        }
        break;

      case MeasureSpec.UNSPECIFIED:
        if (childDimension >= 0) {
          resultSize = childDimension;
          resultMode = MeasureSpec.EXACTLY;
        } else if (childDimension == LayoutParams.MATCH_PARENT) {
          resultSize = size;
          resultMode = MeasureSpec.UNSPECIFIED;
        } else if (childDimension == LayoutParams.WRAP_CONTENT) {
          resultSize = size;
          resultMode = MeasureSpec.UNSPECIFIED;
        }
        break;
    }
    return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
  }

  @Override
  public void requestLayout() {
    mIsLayoutDirty = true;
    super.requestLayout();
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    mIsLayoutDirty = false;
    if (mChildToScrollTo != null && isViewDescendantOf(mChildToScrollTo, this)) {
      scrollToChild(mChildToScrollTo);
    }
    mChildToScrollTo = null;
    if (!isLaidOut()) {
      if (mSavedState != null) {
        setScrollX(mSavedState.scrollPositionX);
        setScrollY(mSavedState.scrollPositionY);
        mSavedState = null;
      }
    }
    scrollTo(getScrollX(), getScrollY());
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    View currentFocused = findFocus();
    if (null == currentFocused || this == currentFocused) {
      return;
    }
    if (isWithinDeltaOfScreen(currentFocused, 0, oldw, oldh)) {
      currentFocused.getDrawingRect(mTempRect);
      offsetDescendantRectToMyCoords(currentFocused, mTempRect);
      int deltaX = computeScrollXDeltaToGetChildRectOnScreen(mTempRect);
      int deltaY = computeScrollYDeltaToGetChildRectOnScreen(mTempRect);
      performScrollBy(deltaX, deltaY);
    }
  }

  // UTILITY

  private boolean inChild(int x, int y) {
    if (hasContent()) {
      final int scrollY = getScrollY();
      final int scrollX = getScrollX();
      final View child = getChild();
      return !(y < child.getTop() - scrollY
          || y >= child.getBottom() - scrollY
          || x < child.getLeft() - scrollX
          || x >= child.getRight() - scrollX);
    }
    return false;
  }

  private boolean isOffScreen(View descendant) {
    return !isWithinDeltaOfScreen(descendant, 0, getWidth(), getHeight());
  }

  private boolean isWithinDeltaOfScreen(View descendant, int delta, int width, int height) {
    descendant.getDrawingRect(mTempRect);
    offsetDescendantRectToMyCoords(descendant, mTempRect);
    return ((mTempRect.bottom + delta) >= getScrollY() && (mTempRect.top - delta) <= (getScrollY() + height))
        && ((mTempRect.right + delta) >= getScrollX() && (mTempRect.left - delta) <= (getScrollX() + width));
  }

  @Override
  public void requestChildFocus(View child, View focused) {
    if (!mIsLayoutDirty) {
      scrollToChild(focused);
    } else {
      mChildToScrollTo = focused;
    }
    super.requestChildFocus(child, focused);
  }

  @Override
  protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
    if (direction == View.FOCUS_FORWARD) {
      direction = View.FOCUS_DOWN;
    } else if (direction == View.FOCUS_BACKWARD) {
      direction = View.FOCUS_UP;
    }
    final View nextFocus = previouslyFocusedRect == null ? FocusFinder.getInstance().findNextFocus(this, null, direction) : FocusFinder.getInstance().findNextFocusFromRect(this, previouslyFocusedRect, direction);
    if (nextFocus == null) {
      return false;
    }
    if (isOffScreen(nextFocus)) {
      return false;
    }
    return nextFocus.requestFocus(direction, previouslyFocusedRect);
  }

  @Override
  public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
    rectangle.offset(child.getLeft() - child.getScrollX(), child.getTop() - child.getScrollY());
    return scrollToChildRect(rectangle, immediate);
  }

  private static boolean isViewDescendantOf(View child, View parent) {
    if (child == parent) {
      return true;
    }
    final ViewParent parentOfChild = child.getParent();
    return (parentOfChild instanceof ViewGroup) && isViewDescendantOf((View) parentOfChild, parent);
  }


  // INPUT & ACCESSIBILITY

  @Override
  public boolean onGenericMotionEvent(MotionEvent event) {
    if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0) {
      switch (event.getAction()) {
        case MotionEvent.ACTION_SCROLL: {
          if (!mIsBeingDragged) {
            final float vscroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            final float hscroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
            if (vscroll != 0) {
              final int verticalScrollRange = getVerticalScrollRange();
              final int horizontalScrollRange = getHorizontalScrollRange();
              int oldScrollY = getScrollY();
              int oldScrollX = getScrollX();
              int newScrollY = (int) (oldScrollY - vscroll);
              int newScrollX = (int) (oldScrollX - hscroll);
              if (newScrollY < 0) {
                newScrollY = 0;
              } else if (newScrollY > verticalScrollRange) {
                newScrollY = verticalScrollRange;
              }
              if (newScrollX < 0) {
                newScrollX = 0;
              } else if (newScrollX > horizontalScrollRange) {
                newScrollX = horizontalScrollRange;
              }
              if (newScrollY != oldScrollY || newScrollX != oldScrollX) {
                super.scrollTo(newScrollX, newScrollY);
                return true;
              }
            }
          }
        }
      }
    }
    return super.onGenericMotionEvent(event);
  }

  @Override
  public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
    super.onInitializeAccessibilityEvent(event);
    event.setClassName(ScrollView.class.getName());
    event.setScrollable(canScroll());
    event.setScrollX(getScrollX());
    event.setScrollY(getScrollY());
    event.setMaxScrollX(getHorizontalScrollRange());
    event.setMaxScrollY(getVerticalScrollRange());
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    return super.dispatchKeyEvent(event) || executeKeyEvent(event);
  }

  public boolean executeKeyEvent(KeyEvent event) {
    if (!canScroll()) {
      if (isFocused()) {
        View currentFocused = findFocus();
        if (currentFocused == this) {
          currentFocused = null;
        }
        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, View.FOCUS_DOWN);
        return nextFocused != null && nextFocused != this && nextFocused.requestFocus(View.FOCUS_DOWN);
      }
      return false;
    }
    boolean alt = event.isAltPressed();
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      switch (event.getKeyCode()) {
        case KeyEvent.KEYCODE_DPAD_UP:
          if (canScrollVertically(DIRECTION_BACKWARD)) {
            if (alt) {
              performScrollBy(0, -getScrollY());
            } else {
              performScrollBy(0, -getHeight());
            }
            return true;
          }
          break;
        case KeyEvent.KEYCODE_DPAD_DOWN:
          // if we can scroll down
          if (canScrollVertically(DIRECTION_FORWARD)) {
            // if alt is down, scroll all the way to the end of content
            if (alt) {
              performScrollBy(0, getChild().getMeasuredHeight() - getScrollY());
            } else {  // otherwise scroll down one "page" (height)
              performScrollBy(0, getHeight());
            }
            return true;
          }
          break;
        case KeyEvent.KEYCODE_DPAD_LEFT:
          // if we can scroll left
          if (canScrollHorizontally(DIRECTION_BACKWARD)) {
            // if alt is down, scroll all the way home
            if (alt) {
              performScrollBy(0, -getScrollX());
            } else {  // otherwise scroll left one "page" (width)
              performScrollBy(0, -getWidth());
            }
            return true;
          }
          break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
          // if we can scroll right
          if (canScrollHorizontally(DIRECTION_FORWARD)) {
            // if alt is down, scroll all the way to the end of content
            if (alt) {
              performScrollBy(getChild().getMeasuredWidth() - getScrollX(), 0);
            } else {  // otherwise scroll right one "page" (width)
              performScrollBy(getWidth(), 0);
            }
            return true;
          }
          break;
      }
    }
    return false;
  }

  // STATE

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    SavedState ss = (SavedState) state;
    super.onRestoreInstanceState(ss.getSuperState());
    mSavedState = ss;
    requestLayout();
  }

  @Override
  protected Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    SavedState ss = new SavedState(superState);
    ss.scrollPositionY = getScrollY();
    ss.scrollPositionX = getScrollX();
    return ss;
  }

  static class SavedState extends BaseSavedState {
    public int scrollPositionY;
    public int scrollPositionX;

    SavedState(Parcelable superState) {
      super(superState);
    }

    public SavedState(Parcel source) {
      super(source);
      scrollPositionX = source.readInt();
      scrollPositionY = source.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      super.writeToParcel(dest, flags);
      dest.writeInt(scrollPositionX);
      dest.writeInt(scrollPositionY);
    }

    @Override
    public String toString() {
      return "ScrollView.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " scrollPositionY=" + scrollPositionY + ", scrollPositionX=" + scrollPositionX + "}";
    }

    public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
      public SavedState createFromParcel(Parcel in) {
        return new SavedState(in);
      }

      public SavedState[] newArray(int size) {
        return new SavedState[size];
      }
    };
  }
}