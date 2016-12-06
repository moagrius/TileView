package com.qozix.tileview.graphics;

import android.graphics.Bitmap;

public class BitmapRecyclerDefault implements BitmapRecycler {
  @Override
  public void recycleBitmap( Bitmap bitmap ) {
    if( !bitmap.isRecycled() ) {
      bitmap.recycle();
    }
  }
}
