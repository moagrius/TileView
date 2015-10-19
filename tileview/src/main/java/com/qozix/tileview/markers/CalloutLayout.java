package com.qozix.tileview.markers;

import android.content.Context;
import android.view.MotionEvent;

public class CalloutLayout extends MarkerLayout {

  public CalloutLayout( Context context ) {
    super( context );
  }

  @Override
  public boolean onTouchEvent( MotionEvent event ) {
    removeAllViews();
    return super.onTouchEvent( event );
  }

}
