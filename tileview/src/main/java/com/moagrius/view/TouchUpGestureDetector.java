package com.moagrius.view;

import android.view.MotionEvent;

public class TouchUpGestureDetector {

  private OnTouchUpListener mOnTouchUpListener;

  public TouchUpGestureDetector(OnTouchUpListener listener) {
    mOnTouchUpListener = listener;
  }

  public boolean onTouchEvent(MotionEvent event) {
    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
      if (mOnTouchUpListener != null) {
        return mOnTouchUpListener.onTouchUp(event);
      }
    }
    return true;
  }

  public interface OnTouchUpListener {
    boolean onTouchUp(MotionEvent event);
  }

}
