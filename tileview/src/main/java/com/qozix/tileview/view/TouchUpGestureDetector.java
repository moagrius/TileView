package com.qozix.tileview.view;

import android.view.MotionEvent;

/**
 * @author Mike Dunn, 10/6/15.
 */
public class TouchUpGestureDetector {

  private OnTouchUpListener mOnTouchUpListener;

  public TouchUpGestureDetector( OnTouchUpListener listener ) {
    mOnTouchUpListener = listener;
  }

  public boolean onTouchEvent( MotionEvent event ) {
    if( event.getActionMasked() == MotionEvent.ACTION_UP ) {
      if( mOnTouchUpListener != null ) {
        return mOnTouchUpListener.onTouchUp( event );
      }
    }
    return true;
  }

  public interface OnTouchUpListener {
    boolean onTouchUp( MotionEvent event );
  }
}

