package com.qozix.tileview.graphics;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.qozix.tileview.tiles.Tile;

import java.io.InputStream;

/**
 * This is a very simple implementation of BitmapProvider, using a formatted string to find
 * an asset by filename, and built-in methods to decode the bitmap data.
 *
 * Feel free to use your own implementation here, where you might implement a favorite library like
 * Picasso, or add your own disk-caching scheme, etc.
 */

public class BitmapProviderAssets implements BitmapProvider {

  private static final BitmapFactory.Options OPTIONS = new BitmapFactory.Options();

  static {
    OPTIONS.inPreferredConfig = Bitmap.Config.RGB_565;
  }

  @Override
  public Bitmap getBitmap( Tile tile, Context context ) {
    Object data = tile.getData();
    if( data instanceof String ) {
      String unformattedFileName = (String) tile.getData();
      String formattedFileName = String.format( unformattedFileName, tile.getColumn(), tile.getRow() );
      AssetManager assetManager = context.getAssets();
      try {
        InputStream inputStream = assetManager.open( formattedFileName );
        if( inputStream != null ) {
          try {
            return BitmapFactory.decodeStream( inputStream, null, OPTIONS );
          } catch( OutOfMemoryError | Exception e ) {
            // this is probably an out of memory error - you can try sleeping (this method won't be called in the UI thread) or try again (or give up)
          }
        }
      } catch( Exception e ) {
        // this is probably an IOException, meaning the file can't be found
      }
    }
    return null;
  }


}
