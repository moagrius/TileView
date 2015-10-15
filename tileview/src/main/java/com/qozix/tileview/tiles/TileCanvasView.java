package com.qozix.tileview.tiles;

import android.content.Context;
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
  private TileCanvasDrawListener mTileCanvasDrawListener;

  public TileCanvasView( Context context ) {
    super( context );
  }

  public void addTile(Tile tile){
    tile.setParentList( mTiles );
    Log.d( "Tiles", "tile count=" + mTiles.size() );
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

  public void setTileCanvasDrawListener( TileCanvasDrawListener tileCanvasDrawListener ) {
    mTileCanvasDrawListener = tileCanvasDrawListener;
  }

  private boolean drawTiles( Canvas canvas ) {
    boolean dirty = false;
    for( Tile tile : mTiles ) {
      dirty = tile.draw( canvas ) || dirty;
    }
    return dirty;
  }

  private void handleDrawState( boolean dirty ){
    if(dirty){
      Log.d( "TileView", "dirty, calling invalidate again" );
      invalidate();
      mHasBeenDirtySinceLastCleanDraw = true;
    } else {
      if( mTileCanvasDrawListener != null ) {
        if( mHasBeenDirtySinceLastCleanDraw ) {
          mTileCanvasDrawListener.onCleanDrawComplete( this );
          mHasBeenDirtySinceLastCleanDraw = false;
        }
      }
    }
  }

  private boolean mHasBeenDirtySinceLastCleanDraw;

  @Override
  public void onDraw( Canvas canvas ) {
    super.onDraw( canvas );
    canvas.save();
    canvas.scale( mScale, mScale );
    boolean dirty = drawTiles( canvas );
    canvas.restore();
    handleDrawState( dirty );
  }

  /**
   * will only fire if transitions are enabled, the first time a "clean" draw is complete after a dirty draw
   */
  public interface TileCanvasDrawListener {
    void onCleanDrawComplete( TileCanvasView tileCanvasView );
  }

}
