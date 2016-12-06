package com.qozix.tileview.graphics;

import android.graphics.Bitmap;

/**
 * This interface represents the final operations applied to a {@link Bitmap} owned by a
 * {@link com.qozix.tileview.tiles.Tile} after it is no longer being used. Generally, this only
 * entails a call to {@link Bitmap#recycle()}, but it also provides a place to catch {@link Bitmap}
 * instances for reuse in a cache or an object pool.
 */
public interface BitmapRecycler {
  void recycleBitmap( Bitmap bitmap );
}
