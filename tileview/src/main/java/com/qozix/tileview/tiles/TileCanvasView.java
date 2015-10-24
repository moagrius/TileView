package com.qozix.tileview.tiles;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

import java.util.HashSet;

public class TileCanvasView extends View {

  private float mScale = 1;

  private HashSet<Tile> mTiles = new HashSet<Tile>();

  private TileCanvasDrawListener mTileCanvasDrawListener;

  private boolean mHasBeenDirtySinceLastCleanDraw;

  public TileCanvasView( Context context ) {
    super( context );
  }

  public void setScale( float factor ) {
    mScale = factor;
    invalidate();
  }

  public float getScale() {
    return mScale;
  }

  public void setTileCanvasDrawListener( TileCanvasDrawListener tileCanvasDrawListener ) {
    mTileCanvasDrawListener = tileCanvasDrawListener;
  }

  public void addTile( Tile tile ) {
    if( !mTiles.contains( tile ) ) {
      mTiles.add( tile );
      tile.setParentTileCanvasView( this );
      invalidate();
    }
  }

  public void removeTile( Tile tile ) {
    if( mTiles.contains( tile ) ) {
      mTiles.remove( tile );
      tile.setParentTileCanvasView( null );
      invalidate();
    }
  }

  public void clearTiles() {
    HashSet<Tile> condemned = (HashSet<Tile>) mTiles.clone();
    for( Tile tile : condemned ) {
      tile.destroy();
    }
    invalidate();
  }

  private boolean drawTiles( Canvas canvas ) {
    boolean dirty = false;
    for( Tile tile : mTiles ) {
      dirty = tile.draw( canvas ) || dirty;
    }
    return dirty;
  }

  private void handleDrawState( boolean dirty ) {
    if( dirty ) {
      invalidate();
      mHasBeenDirtySinceLastCleanDraw = true;
    } else {
      if( mHasBeenDirtySinceLastCleanDraw ) {
        mHasBeenDirtySinceLastCleanDraw = false;
        if( mTileCanvasDrawListener != null ) {
          mTileCanvasDrawListener.onCleanDrawComplete( this );
        }
      }
    }
  }

  @Override
  public void onDraw( Canvas canvas ) {
    Log.d( "TileView", "TileCanvasView.onDraw: " + canvas.getWidth() + ", " + canvas.getHeight() );
    canvas.scale( mScale, mScale );
    boolean dirty = drawTiles( canvas );
    super.onDraw( canvas );
    handleDrawState( dirty );
  }

  /**
   * Interface definition for a callback to be invoked when a "clean" draw occurs.
   * The callback will only be invoked if transitions are enabled, and will occur
   * the first time a "clean" draw is complete after a "dirty" draw (a dirty draw
   * is defined as an onDraw invocation that requires a subsequent call to invalidate).
   */
  public interface TileCanvasDrawListener {
    void onCleanDrawComplete( TileCanvasView tileCanvasView );
  }

}
