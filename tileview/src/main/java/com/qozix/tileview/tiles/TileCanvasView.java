package com.qozix.tileview.tiles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by q on 10/2/15.
 */
public class TileCanvasView extends View {

  private float mScale = 1;
  private LinkedList<Tile> mTiles = new LinkedList<Tile>();

  public TileCanvasView( Context context ) {
    super( context );
  }

  public void addTile(Tile tile){
    tile.setParentList( mTiles );
    Log.d( "TileView", "tile count=" + mTiles.size() );
    postInvalidate();
  }

  public void removeTile(Tile tile){
    tile.setParentList( null );
    postInvalidate();
  }

  public void clearTiles(){
    List<Tile> condemned = (List<Tile>) mTiles.clone();
    for( Tile tile : condemned ) {
      tile.destroy();
    }
  }

  public void setScale( float factor ) {
    mScale = factor;
    postInvalidate();
  }

  public float getScale() {
    return mScale;
  }


  @Override
  public void onDraw( Canvas canvas ) {
    super.onDraw( canvas );
    canvas.save();
    canvas.scale( mScale, mScale );
    boolean dirty = false;
    for( Tile tile : mTiles ) {
      Bitmap bitmap = tile.getBitmap();
      if( bitmap != null ) {
        canvas.drawBitmap( bitmap, tile.getLeft(), tile.getTop(), tile.getPaint() );
        if(!dirty){
          dirty = tile.getIsDirty();
        }
      }
    }
    canvas.restore();
    if(dirty){
      Log.d( "TileView", "dirty, calling invalidate again" );
      invalidate();
    }
  }
}
