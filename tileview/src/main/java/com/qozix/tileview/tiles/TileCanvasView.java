package com.qozix.tileview.tiles;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import java.util.HashSet;

public class TileCanvasView extends View {

  private float mScale = 1;

  private HashSet<Tile> mTiles = new HashSet<Tile>();

  private TileCanvasDrawListener mTileCanvasDrawListener;

  private boolean mHasHadPendingUpdatesSinceLastCompleteDraw;

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

  public void clearTiles( boolean shouldRecycle ) {
    HashSet<Tile> condemned = (HashSet<Tile>) mTiles.clone();
    for( Tile tile : condemned ) {
      tile.destroy( shouldRecycle );
    }
    invalidate();
  }

  /**
   * Draw tile bitmaps into the surface canvas displayed by this View.
   * @param canvas The Canvas instance to draw tile bitmaps into.
   * @return True if there are incomplete tile transitions pending, false otherwise.
   */
  private boolean drawTiles( Canvas canvas ) {
    boolean pending = false;
    for( Tile tile : mTiles ) {
      pending = tile.draw( canvas ) || pending;
    }
    return pending;
  }

  /**
   * During a draw operation, if any tiles are transitioning in, the operation is considered pending,
   * and another redraw is requested immediately (via invalidate).
   *
   * NOTE: the invalidate invocation in this method should not be necessary, since the
   * TileCanvasViewGroup that contains this View should also be listening for onDrawPending,
   * at which time it will call invalidate on itself.
   *
   * @param pending True if tile transitions states are not complete, and an immediate redraw is required.
   */
  private void handleDrawState( boolean pending ) {
    if( pending ) {
      invalidate();
      mHasHadPendingUpdatesSinceLastCompleteDraw = true;
      if( mTileCanvasDrawListener != null ) {
        mTileCanvasDrawListener.onDrawPending( this );
      }
    } else {
      if( mHasHadPendingUpdatesSinceLastCompleteDraw ) {
        mHasHadPendingUpdatesSinceLastCompleteDraw = false;
        if( mTileCanvasDrawListener != null ) {
          mTileCanvasDrawListener.onDrawComplete( this );
        }
      }
    }
  }

  @Override
  public void onDraw( Canvas canvas ) {
    super.onDraw( canvas );
    canvas.save();
    canvas.scale( mScale, mScale );
    boolean pending = drawTiles( canvas );
    canvas.restore();
    handleDrawState( pending );
  }

  /**
   * Interface definition for callbacks to be invoked when drawing is complete.
   * A "pending" draw is one that indicates tile transitions states are still pending and
   * and immediate redraw should be requested.
   * A "complete" draw callback will only be invoked if transitions are enabled, and will occur
   * the first time a "complete" draw is complete after a "pending" draw.
   */
  public interface TileCanvasDrawListener {
    void onDrawComplete( TileCanvasView tileCanvasView );
    void onDrawPending( TileCanvasView tileCanvasView );
  }

}
