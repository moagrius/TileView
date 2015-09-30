package com.qozix.tileview.graphics;

import android.content.Context;
import android.graphics.Bitmap;

import com.qozix.tileview.tiles.Tile;

public interface BitmapDecoder {

	public Bitmap decode( Tile tile, Context context );

}
