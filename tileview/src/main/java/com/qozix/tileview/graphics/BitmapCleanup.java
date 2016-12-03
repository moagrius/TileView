package com.qozix.tileview.graphics;

import android.graphics.Bitmap;

/**
 * This interface represents the "cleanup" operations needed for the Bitmap of a Tile after it is no
 * longer needed.  Generally, this only entails a call to {@link Bitmap#recycle()}, but it also
 * provides a place to catch Bitmap instances to recycle in an object pool.
 */
public interface BitmapCleanup {
  void cleanupBitmap( Bitmap b );
}
