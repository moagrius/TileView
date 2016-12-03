package com.qozix.tileview.graphics;

import android.graphics.Bitmap;

public class BitmapCleanupRecycle implements BitmapCleanup {
  @Override
  public void cleanupBitmap( Bitmap b ) {
    b.recycle();
  }
}
