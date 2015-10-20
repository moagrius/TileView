package com.qozix.tileview.tiles;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import java.util.HashSet;

/**
 * Created by q on 10/2/15.
 */
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
