package com.qozix.tileview.graphics;

import android.content.Context;
import android.graphics.Bitmap;

import com.qozix.tileview.tiles.Tile;

/**
 * This interface provides the bitmap data necessary to draw each tile.  It will be run
 * in a worker (non-UI) thread, so network operations are safe here.  How the bitmap is generated
 * is entirely up to you - assets, resources, file system, network access, SVG, drawn dynamically
 * with Canvas instances, it doesn't matter - the method is passed a Tile instance (with information
 * like column and row numbers, including the arbitrary data object passed for the detail level),
 * and a Context instance to help with things like file i/o - as long as the
 * getBitmap method returns a bitmap, everything will run along nicely.
 */
public interface BitmapProvider {
  Bitmap getBitmap( Tile tile, Context context );
}
