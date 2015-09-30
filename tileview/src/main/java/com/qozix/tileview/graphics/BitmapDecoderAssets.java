package com.qozix.tileview.graphics;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.qozix.tileview.tiles.Tile;

import java.io.IOException;
import java.io.InputStream;

public class BitmapDecoderAssets implements BitmapDecoder {

	private static final BitmapFactory.Options OPTIONS = new BitmapFactory.Options();
	static {
		OPTIONS.inPreferredConfig = Bitmap.Config.RGB_565;
	}
	
	@Override
	public Bitmap decode( Tile tile, Context context ) {
		Object data = tile.getData();
		if(data instanceof String){
			String fileName = (String) tile.getData();
			fileName = String.format(fileName, tile.getRow(), tile.getCol());
			Log.d( "DEBUG", fileName );
			AssetManager assets = context.getAssets();
			try {
				InputStream input = assets.open( fileName );
				if ( input != null ) {
					try {
						return BitmapFactory.decodeStream( input, null, OPTIONS );
					} catch ( OutOfMemoryError oom ) {
						// oom - you can try sleeping (this method won't be called in the UI thread) or try again (or give up)
					} catch ( Exception e ) {
						// unknown error decoding bitmap
					}
				}
			} catch ( IOException io ) {
				// io error - probably can't find the file
			} catch ( Exception e ) {
				// unknown error opening the asset
			}
		}

		return null;
	}


}
