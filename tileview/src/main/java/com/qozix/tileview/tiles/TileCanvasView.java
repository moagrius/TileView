package com.qozix.tileview.tiles;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

import java.util.HashSet;

/**
 * Created by q on 10/2/15.
 */
public class TileCanvasView extends View {

  private float mScale = 1;
  private HashSet<Tile> mTiles = new HashSet<Tile>();
  private TileCanvasDrawListener mTileCanvasDrawListener;

  public TileCanvasView( Context context ) {
    super( context );
  }

  public void addTile(Tile tile){
    if( !mTiles.contains( tile ) ) {
      mTiles.add( tile );
      tile.setParentTileCanvasView( this );
      invalidate();
    }
    Log.d( "Tiles", "tile count=" + mTiles.size() );
  }

  public void removeTile(Tile tile){
    if( mTiles.contains( tile ) ) {
      mTiles.remove( tile );
      tile.setParentTileCanvasView( null );
      invalidate();
    }
  }

  public void clearTiles(){
    HashSet<Tile> condemned = (HashSet<Tile>) mTiles.clone();
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
