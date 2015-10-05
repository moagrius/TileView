package com.qozix.tileview.tiles;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import java.util.LinkedList;

/**
 * Created by q on 10/2/15.
 */
public class TileCanvasView extends View {

  private float mScale = 1;
  private LinkedList<Tile> mTilesToRender;

  public TileCanvasView( Context context ) {
    super( context );
  }

  public void setTiles(LinkedList<Tile> tiles){
    mTilesToRender = tiles;
  }

  public void setScale( float factor ) {
    mScale = factor;
    invalidate();
  }

  public float getScale() {
    return mScale;
  }

  @Override
  public void onDraw( Canvas canvas ) {
    for( Tile tile : mTilesToRender ) {
      canvas.drawBitmap( tile.getBitmap(), tile.getLeft(), tile.getTop(), null );
    }
    canvas.scale( mScale, mScale );
    super.onDraw( canvas );
  }
}
