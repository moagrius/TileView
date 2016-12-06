package com.qozix.tileview.graphics;

import android.graphics.Bitmap;

public class BitmapRecyclerDefault implements BitmapRecycler {
  @Override
  public void recycleBitmap(Bitmap soylentGreen ) {
    if( ! soylentGreen.isRecycled() ) {
      soylentGreen.recycle();
    }
  }
}
