package com.qozix.tileview.tiles;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * @author Mike Dunn, 3/10/16.
 */
class TileRenderHandler extends Handler {

  public static final int RENDER_ERROR = -1;
  public static final int RENDER_INCOMPLETE = 0;
  public static final int RENDER_COMPLETE = 1;

  public enum Status {

    ERROR (RENDER_ERROR),
    INCOMPLETE (RENDER_INCOMPLETE),
    COMPLETE (RENDER_COMPLETE);

    private int mMessageCode;

    Status(int messageCode){
      mMessageCode = messageCode;
    }
    int getMessageCode(){
      return mMessageCode;
    }
  }


  public TileRenderHandler() {
    this( Looper.getMainLooper() );
  }

  public TileRenderHandler( Looper looper ) {
    super( looper );
  }

  @Override
  public void handleMessage( Message message ) {
    TileRenderRunnable tileRenderRunnable = (TileRenderRunnable) message.obj;
    TileCanvasViewGroup tileCanvasViewGroup = tileRenderRunnable.getTileCanvasViewGroup();
    if( tileCanvasViewGroup == null ) {
      return;
    }
    Tile tile = tileRenderRunnable.getTile();
    if( tile == null ) {
      return;
    }
    switch( message.what ) {
      case RENDER_ERROR :
        tileCanvasViewGroup.handleTileRenderException( tileRenderRunnable.getThrowable() );
        break;
      case RENDER_COMPLETE:
        tileCanvasViewGroup.addTileToCurrentTileCanvasView( tile );
        break;
      case RENDER_INCOMPLETE:
        // Tile was queued but was not rendered, but no Error or Exception was thrown
        break;
    }
  }
}
