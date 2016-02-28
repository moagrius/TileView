package com.qozix.tileview.tiles;

import android.os.Process;

import java.io.InterruptedIOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TileRenderPoolManager extends ThreadPoolExecutor {

  private static final int KEEP_ALIVE_TIME = 1;
  private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

  private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
  private static final int MAXIMUM_POOL_SIZE = Runtime.getRuntime().availableProcessors();

  private final HashMap<Future, TileRenderRunnable> mFutureTileRenderRunnableHashMap = new HashMap<>();
  private WeakReference<TileCanvasViewGroup> mTileCanvasViewGroupWeakReference;
  private List<Tile> mCurrentRenderingTiles = new LinkedList<>();
  private List<Future> mCancelledFutures = new LinkedList<>();

  public TileRenderPoolManager() {
    super(
      CORE_POOL_SIZE,
      MAXIMUM_POOL_SIZE,
      KEEP_ALIVE_TIME,
      KEEP_ALIVE_TIME_UNIT,
      new LinkedBlockingDeque<Runnable>()
    );
  }

  public boolean isShutdownOrTerminating() {
    return isShutdown() || isTerminating() || isTerminated();
  }

  @Override
  protected void afterExecute( Runnable runnable, Throwable throwable ) {
    super.afterExecute( runnable, throwable );
    if( mRunnableLinkedBlockingDeque.size() == 0 && getActiveCount() == 1 ) {
      mFutureTileRenderRunnableHashMap.clear();
      TileCanvasViewGroup tileCanvasViewGroup = mTileCanvasViewGroupWeakReference.get();
      if( tileCanvasViewGroup != null ) {
        tileCanvasViewGroup.onRenderTaskPostExecute();
      }
    }
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    return new FutureTask<T>(runnable, value);
  }

  public void cancel() {
    synchronized( this ) {
      if( mRunnableLinkedBlockingDeque.size() > 0 || mTileRenderThreadPoolExecutor.getActiveCount() > 0 ) {
        TileCanvasViewGroup tileCanvasViewGroup = mTileCanvasViewGroupWeakReference.get();
        if( tileCanvasViewGroup != null ) {
          tileCanvasViewGroup.onRenderTaskCancelled();
        }
      }
      for( Map.Entry<Future, TileRenderRunnable> entry : mFutureTileRenderRunnableHashMap.entrySet() ) {
        Future future = entry.getKey();
        if( !future.isDone() ) {
          TileRenderRunnable tileRenderRunnable = entry.getValue();
          tileRenderRunnable.cancel();
        }
      }
      mRunnableLinkedBlockingDeque.clear();
      mFutureTileRenderRunnableHashMap.clear();
    }
  }

  public void shutdown() {
    cancel();
    mTileRenderThreadPoolExecutor.shutdownNow();
  }

  public void queue( TileCanvasViewGroup tileCanvasViewGroup, List<Tile> renderList ) {
    renderList.removeAll( mCurrentRenderingTiles );
    mCurrentRenderingTiles.addAll( renderList );
    cancel();  // TODO: up one line?
    if( renderList.size() > 0 ) {
      mTileCanvasViewGroupWeakReference = new WeakReference<>( tileCanvasViewGroup );
      tileCanvasViewGroup.onRenderTaskPreExecute();
      for( Tile tile : renderList ) {
        if( mTileRenderThreadPoolExecutor.isShutdownOrTerminating() ) {
          return;
        }
        TileRenderRunnable tileRenderRunnable = new TileRenderRunnable( tileCanvasViewGroup, tile );
        tileRenderRunnable.setTileRenderRunnableCompleteListener( mTileRenderRunnableCompleteListener );
        Future future = mTileRenderThreadPoolExecutor.submit( tileRenderRunnable );
        mFutureTileRenderRunnableHashMap.put( future, tileRenderRunnable );
      }
    }
  }

  private TileRenderRunnable.TileRenderRunnableCompleteListener mTileRenderRunnableCompleteListener = new TileRenderRunnable.TileRenderRunnableCompleteListener() {
    @Override
    public void onRenderComplete( Tile tile ) {
      mCurrentRenderingTiles.remove( tile );
    }
  };

  private static class TileRenderRunnable implements Runnable {

    interface TileRenderRunnableCompleteListener {
      public void onRenderComplete( Tile tile );
    }

    private final WeakReference<TileCanvasViewGroup> mTileCanvasViewGroup;
    private final WeakReference<Tile> mTileWeakReference;

    private TileRenderRunnableCompleteListener mTileRenderRunnableCompleteListener;

    private volatile boolean mCancelled = false;

    public TileRenderRunnable( TileCanvasViewGroup viewGroup, Tile tile ) {
      mTileCanvasViewGroup = new WeakReference<>( viewGroup );
      mTileWeakReference = new WeakReference<>( tile );
    }

    public void cancel() {
      mCancelled = true;
    }

    public void setTileRenderRunnableCompleteListener( TileRenderRunnableCompleteListener tileRenderRunnableCompleteListener ) {
      mTileRenderRunnableCompleteListener = tileRenderRunnableCompleteListener;
    }

    @Override
    public void run() {
      if( mCancelled ) {
        return;
      }
      Process.setThreadPriority( Process.THREAD_PRIORITY_BACKGROUND );
      final Thread thread = Thread.currentThread();
      if( thread.isInterrupted() ) {
        return;
      }
      TileCanvasViewGroup tileCanvasViewGroup = mTileCanvasViewGroup.get();
      if( tileCanvasViewGroup == null ) {
        return;
      }
      if( tileCanvasViewGroup.getRenderIsCancelled() ) {
        return;
      }
      Tile tile = mTileWeakReference.get();
      if( tile == null ) {
        return;
      }
      try {
        tileCanvasViewGroup.generateTileBitmap( tile );
      } catch( InterruptedIOException e ) {
        Thread.currentThread().interrupt();
        return;
      } catch( Exception e ) {
        return;
      }
      if( mTileRenderRunnableCompleteListener != null ) {
        mTileRenderRunnableCompleteListener.onRenderComplete( tile );
      }
      if( mCancelled || tile.getBitmap() == null || thread.isInterrupted() || tileCanvasViewGroup.getRenderIsCancelled() ) {
        tile.destroy( true );
        return;
      }
      tileCanvasViewGroup.addTileToCurrentTileCanvasView( tile );
    }
  }
}
