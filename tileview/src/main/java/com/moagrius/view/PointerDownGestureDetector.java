package com.moagrius.view;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class PointerDownGestureDetector {

  public interface OnPointerDownListener {
    void onDoubleTap(MotionEvent event);
    void onPointerCounterChange(int pointerCount);
  }

  private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();

  private OnPointerDownListener mOnPointerDownListener;
  private MotionEvent mLastDownEvent;

  private long mLastDownTimestamp;
  private int mDoubleTapSlopSquare;
  private int mLastPointerCount;

  public PointerDownGestureDetector(Context context, OnPointerDownListener listener) {
    mOnPointerDownListener = listener;
    int doubleTapSlop = ViewConfiguration.get(context).getScaledDoubleTapSlop();
    mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;
  }

  private void notifyPointerCountChanged(int pointerCount) {
    if (mOnPointerDownListener != null && mLastPointerCount != pointerCount) {
      mOnPointerDownListener.onPointerCounterChange(pointerCount);
      mLastPointerCount = pointerCount;
    }
  }

  public boolean onTouchEvent(MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_POINTER_DOWN:
        notifyPointerCountChanged(event.getPointerCount());
        return false;
      case MotionEvent.ACTION_DOWN:
        notifyPointerCountChanged(event.getPointerCount());
        Log.d("DT", "action down");
        if (mLastDownEvent == null) {
          Log.d("DT", "first tap");
          register(event);
          return false;
        }
        // if not null, this could be the second tap
        // fist, see if it happened fast enough
        long elapsed = System.currentTimeMillis() - mLastDownTimestamp;
        Log.d("DT", "elapsed=" + elapsed + ", window=" + DOUBLE_TAP_TIMEOUT);
        if (elapsed > DOUBLE_TAP_TIMEOUT) {
          Log.d("DT", "took too long, register and return false");
          register(event);
          return false;
        }
        // make sure the finger didn't wander too far
        final int deltaX = (int) (event.getX() - mLastDownEvent.getX());
        final int deltaY = (int) (event.getY() - mLastDownEvent.getY());
        int distance = (deltaX * deltaX) + (deltaY * deltaY);
        Log.d("DT", "deltaX=" + deltaX + ", deltaY=" + deltaY + ", distance=" + distance);
        if (distance > mDoubleTapSlopSquare) {
          Log.d("DT", "wandered too far, reset and return false");
          register(event);
          return false;
        }
        // we made it this far, so it didn't wander and happened within proscribed delay
        // it's a double tap
        Log.d("DT", "made it this far, send double tap event and return true");
        mOnPointerDownListener.onDoubleTap(event);
        reset();
        return true;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_POINTER_UP:
        notifyPointerCountChanged(event.getPointerCount());
        return false;
      case MotionEvent.ACTION_CANCEL:
        notifyPointerCountChanged(event.getPointerCount());
        Log.d("DT", "action cancel, reset and return false");
        reset();
        return false;
    }
    Log.d("DT", "return false for gesture we don't care about");
    return false;
  }

  private void register(MotionEvent event) {
    mLastDownEvent = event;
    mLastDownTimestamp = System.currentTimeMillis();
  }

  private void reset() {
    mLastDownTimestamp = 0;
    mLastDownEvent = null;
  }

}


