package com.qozix.tileview.tiles;

import android.os.Handler;
import android.os.Message;
import android.os.Process;

import java.lang.ref.WeakReference;

/**
 * @author Mike Dunn, 3/10/16.
 */
class TileRenderRunnable implements Runnable {

  private WeakReference<Tile> mTileWeakReference;
  private WeakReference<TileRenderPoolExecutor> mTileRenderPoolExecutorWeakReference;

  private boolean mCancelled = false;
  private boolean mComplete = false;

  private volatile Thread mThread;

  private Throwable mThrowable;

  public boolean cancel( boolean mayInterrupt ) {
    if( mayInterrupt && mThread != null ) {
      mThread.interrupt();
    }
    boolean cancelled = mCancelled;
    mCancelled = true;
    if( mTileRenderPoolExecutorWeakReference != null ) {
      TileRenderPoolExecutor tileRenderPoolExecutor = mTileRenderPoolExecutorWeakReference.get();
      if(tileRenderPoolExecutor != null){
        tileRenderPoolExecutor.remove( this );
      }
    }
    return !cancelled;
  }

  public boolean isCancelled() {
    return mCancelled;
  }

  public boolean isDone() {
    return mComplete;
  }

  public void setTileRenderPoolExecutor(TileRenderPoolExecutor tileRenderPoolExecutor ) {
    mTileRenderPoolExecutorWeakReference = new WeakReference<>(tileRenderPoolExecutor);
  }

  public void setTile( Tile tile ) {
    mTileWeakReference = new WeakReference<>( tile );
  }

  public Tile getTile() {
    if( mTileWeakReference != null ) {
      return mTileWeakReference.get();
    }
    return null;
  }

  public Throwable getThrowable() {
    return mThrowable;
  }

  public TileRenderHandler.Status renderTile() {
    if( mCancelled ) {
      return TileRenderHandler.Status.INCOMPLETE;
    }
    android.os.Process.setThreadPriority( Process.THREAD_PRIORITY_BACKGROUND );
    if( mThread.isInterrupted() ) {
      return TileRenderHandler.Status.INCOMPLETE;
    }
    Tile tile = getTile();
    if( tile == null ) {
      return TileRenderHandler.Status.INCOMPLETE;
    }
    TileRenderPoolExecutor tileRenderPoolExecutor = mTileRenderPoolExecutorWeakReference.get();
    if( tileRenderPoolExecutor == null ) {
      return TileRenderHandler.Status.INCOMPLETE;
    }
    TileCanvasViewGroup tileCanvasViewGroup = tileRenderPoolExecutor.getTileCanvasViewGroup();
    if(tileCanvasViewGroup == null ) {
      return TileRenderHandler.Status.INCOMPLETE;
    }
    try {
      tile.generateBitmap( tileCanvasViewGroup.getContext(), tileCanvasViewGroup.getBitmapProvider() );
    } catch( Throwable throwable ) {
      mThrowable = throwable;
      return TileRenderHandler.Status.ERROR;
    }
    if( mCancelled || tile.getBitmap() == null || mThread.isInterrupted() ) {
      tile.reset();
      return TileRenderHandler.Status.INCOMPLETE;
    }
    return TileRenderHandler.Status.COMPLETE;
  }

  @Override
  public void run() {
    mThread = Thread.currentThread();
    TileRenderHandler.Status status = renderTile();
    if( status == TileRenderHandler.Status.INCOMPLETE ) {
      return;
    }
    if( status == TileRenderHandler.Status.COMPLETE ) {
      mComplete = true;
    }
    TileRenderPoolExecutor tileRenderPoolExecutor = mTileRenderPoolExecutorWeakReference.get();
    if( tileRenderPoolExecutor != null ) {
      TileCanvasViewGroup tileCanvasViewGroup = tileRenderPoolExecutor.getTileCanvasViewGroup();
      if( tileCanvasViewGroup != null ) {
        Tile tile = getTile();
        if( tile != null ) {
          Handler handler = tileRenderPoolExecutor.getHandler();
          if( handler != null ) {
            // need to stamp time now, since it'll be drawn before the handler posts
            tile.setTransitionsEnabled( tileCanvasViewGroup.getTransitionsEnabled() );
            tile.setTransitionDuration( tileCanvasViewGroup.getTransitionDuration() );
            Message message = handler.obtainMessage( status.getMessageCode(), this );
            message.sendToTarget();
          }
        }
      }

    }
  }

}
