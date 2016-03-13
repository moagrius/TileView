package com.qozix.tileview.tiles;

import android.content.Context;

import com.qozix.tileview.graphics.BitmapProvider;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TileRenderPoolExecutor extends ThreadPoolExecutor {

  private static final int KEEP_ALIVE_TIME = 1;
  private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

  private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
  private static final int INITIAL_POOL_SIZE = AVAILABLE_PROCESSORS >> 1;
  private static final int MAXIMUM_POOL_SIZE = AVAILABLE_PROCESSORS;

  private WeakReference<TileCanvasViewGroup> mTileCanvasViewGroupWeakReference;

  private TileRenderHandler mHandler = new TileRenderHandler();

  public TileRenderPoolExecutor() {
    super(
      INITIAL_POOL_SIZE,
      MAXIMUM_POOL_SIZE,
      KEEP_ALIVE_TIME,
      KEEP_ALIVE_TIME_UNIT,
      new LinkedBlockingDeque<Runnable>()
    );
  }

  public void queue( TileCanvasViewGroup tileCanvasViewGroup, Set<Tile> renderSet ) {
    mTileCanvasViewGroupWeakReference = new WeakReference<>( tileCanvasViewGroup );
    mHandler.setTileCanvasViewGroup( tileCanvasViewGroup );
    final Context context = tileCanvasViewGroup.getContext();
    final BitmapProvider bitmapProvider = tileCanvasViewGroup.getBitmapProvider();
    tileCanvasViewGroup.onRenderTaskPreExecute();
    for( Runnable runnable : getQueue() ) {
      if( runnable instanceof TileRenderRunnable ) {
        TileRenderRunnable tileRenderRunnable = (TileRenderRunnable) runnable;
        if( tileRenderRunnable.isDone() || tileRenderRunnable.isCancelled() ) {
          continue;
        }
        Tile tile = tileRenderRunnable.getTile();
        if( tile == null ) {
          continue;
        }
        if( renderSet.contains( tile ) ) {
          renderSet.remove( tile );
        } else {
          tileRenderRunnable.cancel( true );
          remove( tileRenderRunnable );
        }
      }
    }
    for( Tile tile : renderSet ) {
      if( isShutdownOrTerminating() ) {
        return;
      }
      TileRenderRunnable runnable = new TileRenderRunnable();
      runnable.setTile( tile );
      runnable.setContext( context );
      runnable.setBitmapProvider( bitmapProvider );
      runnable.setHandler( mHandler );
      execute( runnable );
    }
  }

  private void broadcastCancel() {
    if( mTileCanvasViewGroupWeakReference != null ) {
      TileCanvasViewGroup tileCanvasViewGroup = mTileCanvasViewGroupWeakReference.get();
      if( tileCanvasViewGroup != null ) {
        tileCanvasViewGroup.onRenderTaskCancelled();
      }
    }
  }

  public void cancel() {
    for( Runnable runnable : getQueue() ) {
      if( runnable instanceof TileRenderRunnable ) {
        TileRenderRunnable tileRenderRunnable = (TileRenderRunnable) runnable;
        tileRenderRunnable.cancel( true );
      }
    }
    getQueue().clear();
    broadcastCancel();
  }

  public boolean isShutdownOrTerminating() {
    return isShutdown() || isTerminating() || isTerminated();
  }

  @Override
  protected void afterExecute( Runnable runnable, Throwable throwable ) {
    synchronized( this ) {
      super.afterExecute( runnable, throwable );
      if( getQueue().size() == 0 && getActiveCount() == 1 ) {
        TileCanvasViewGroup tileCanvasViewGroup = mTileCanvasViewGroupWeakReference.get();
        if( tileCanvasViewGroup != null ) {
          tileCanvasViewGroup.onRenderTaskPostExecute();
        }
      }
    }
  }

}
