package com.qozix.tileview.tiles;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * @author Mike Dunn, 3/10/16.
 */
class TileRenderHandler extends Handler {

  public static final int RENDER_ERROR = -1;
  public static final int RENDER_INCOMPLETE = 0;
  public static final int RENDER_COMPLETE = 1;

  public enum Status {

    ERROR( RENDER_ERROR ),
    INCOMPLETE( RENDER_INCOMPLETE ),
    COMPLETE( RENDER_COMPLETE );

    private int mMessageCode;

    Status( int messageCode ) {
      mMessageCode = messageCode;
    }

    int getMessageCode() {
      return mMessageCode;
    }

  }

  private WeakReference<TileCanvasViewGroup> mTileCanvasViewGroupWeakReference;

  public TileRenderHandler() {
    this( Looper.getMainLooper() );
  }

  public TileRenderHandler( Looper looper ) {
    super( looper );
  }

  public void setTileCanvasViewGroup( TileCanvasViewGroup tileCanvasViewGroup ) {
    mTileCanvasViewGroupWeakReference = new WeakReference<>( tileCanvasViewGroup );
  }

  public TileCanvasViewGroup getTileCanvasViewGroup() {
    if( mTileCanvasViewGroupWeakReference == null ) {
      return null;
    }
    return mTileCanvasViewGroupWeakReference.get();
  }

  @Override
  public void handleMessage( Message message ) {
    TileRenderRunnable tileRenderRunnable = (TileRenderRunnable) message.obj;
    TileCanvasViewGroup tileCanvasViewGroup = getTileCanvasViewGroup();
    if( tileCanvasViewGroup == null ) {
      return;
    }
    Tile tile = tileRenderRunnable.getTile();
    if( tile == null ) {
      return;
    }
    switch( message.what ) {
      case RENDER_ERROR:
        tileCanvasViewGroup.handleTileRenderException( tileRenderRunnable.getThrowable() );
        break;
      case RENDER_COMPLETE:
        tileCanvasViewGroup.addTileToCanvas( tile );
        break;
    }
  }
}
