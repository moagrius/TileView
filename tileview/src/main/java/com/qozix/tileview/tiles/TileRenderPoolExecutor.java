package com.qozix.tileview.tiles;

import android.os.Process;
import android.support.annotation.NonNull;

import java.io.InterruptedIOException;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TileRenderPoolExecutor extends ThreadPoolExecutor {

  private static final int KEEP_ALIVE_TIME = 1;
  private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

  private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
  private static final int MAXIMUM_POOL_SIZE = Runtime.getRuntime().availableProcessors();

  private WeakReference<TileCanvasViewGroup> mTileCanvasViewGroupWeakReference;

  private List<TileRenderTask> mExecutingTileRenderTasks = new LinkedList<>();
  private List<TileRenderTask> mCancelledTileRenderTasks = new LinkedList<>();

  public TileRenderPoolExecutor() {
    super(
      CORE_POOL_SIZE,
      MAXIMUM_POOL_SIZE,
      KEEP_ALIVE_TIME,
      KEEP_ALIVE_TIME_UNIT,
      new LinkedBlockingDeque<Runnable>()
    );
  }

  public void cancel() {
    if( getQueue().size() > 0 || getActiveCount() > 0 ) {
      TileCanvasViewGroup tileCanvasViewGroup = mTileCanvasViewGroupWeakReference.get();
      if( tileCanvasViewGroup != null ) {
        tileCanvasViewGroup.onRenderTaskCancelled();
      }
    }
    getQueue().clear();
    mCancelledTileRenderTasks.addAll( mExecutingTileRenderTasks );
    mExecutingTileRenderTasks.clear();
    stopCancelledTasks();
  }

  @Override
  public void shutdown() {
    cancel();
    super.shutdown();
  }

  @Override
  @NonNull
  public List<Runnable> shutdownNow() {
    cancel();
    return super.shutdownNow();
  }

  private void stopCancelledTasks() {
    for( TileRenderTask tileRenderTask : mCancelledTileRenderTasks ) {
      tileRenderTask.cancel();
    }
    mCancelledTileRenderTasks.clear();
  }

  public void queue( TileCanvasViewGroup tileCanvasViewGroup, List<Tile> renderList ) {
    for( TileRenderTask tileRenderTask : mExecutingTileRenderTasks ) {
      if( renderList.contains( tileRenderTask.tile ) ) {
        renderList.remove( tileRenderTask.tile );
      } else {
        mCancelledTileRenderTasks.add( tileRenderTask );
      }
    }
    stopCancelledTasks();
    if( renderList.size() > 0 ) {
      mTileCanvasViewGroupWeakReference = new WeakReference<>( tileCanvasViewGroup );
      tileCanvasViewGroup.onRenderTaskPreExecute();
      for( Tile tile : renderList ) {
        if( isShutdownOrTerminating() ) {
          return;
        }
        TileRenderTask task = new TileRenderTask();
        task.tile = tile;
        task.runnable = new TileRenderRunnable();
        task.runnable.setTile( tile );
        task.runnable.setTileCanvasViewGroup( tileCanvasViewGroup );
        task.runnable.setTileRenderPoolExecutor( this );
        task.future = submit( task.runnable );
        mExecutingTileRenderTasks.add( task );

      }
    }
  }

  private TileRenderTask getTaskByTile( Tile tile ) {
    if( tile != null ) {
      for( TileRenderTask tileRenderTask : mExecutingTileRenderTasks ) {
        if( tile.equals( tileRenderTask.tile ) ) {
          return tileRenderTask;
        }
      }
    }
    return null;
  }

  public void removeTaskFromCurrentlyExecutingList( TileRenderTask tileRenderTask ) {
    mExecutingTileRenderTasks.remove( tileRenderTask );
  }

  public boolean isShutdownOrTerminating() {
    return isShutdown() || isTerminating() || isTerminated();
  }

  @Override
  protected void afterExecute( Runnable runnable, Throwable throwable ) {
    super.afterExecute( runnable, throwable );
    if( getQueue().size() == 0 && getActiveCount() == 1 ) {
      TileCanvasViewGroup tileCanvasViewGroup = mTileCanvasViewGroupWeakReference.get();
      if( tileCanvasViewGroup != null ) {
        tileCanvasViewGroup.onRenderTaskPostExecute();
      }
    }
  }

  private static class TileRenderTask {

    public Future future;
    public TileRenderRunnable runnable;
    public Tile tile;

    public void cancel() {
      future.cancel( true );
      runnable.cancel();
      tile.destroy( true );
    }
  }

  private static class TileRenderRunnable implements Runnable {

    private WeakReference<TileRenderPoolExecutor> mTileRenderPoolExecutorWeakReference;
    private WeakReference<TileCanvasViewGroup> mTileCanvasViewGroupWeakReference;
    private WeakReference<Tile> mTileWeakReference;

    private volatile boolean mCancelled = false;

    public TileRenderRunnable() {

    }

    public void markComplete() {
      TileRenderPoolExecutor tileRenderPoolExecutor = getTileRenderPoolExecutor();
      if( tileRenderPoolExecutor != null ) {
        Tile tile = getTile();
        if( tile != null ) {
          TileRenderTask tileRenderTask = tileRenderPoolExecutor.getTaskByTile( tile );
          tileRenderPoolExecutor.removeTaskFromCurrentlyExecutingList( tileRenderTask );
        }
      }
    }

    public void setTileRenderPoolExecutor( TileRenderPoolExecutor tileRenderPoolExecutor ) {
      mTileRenderPoolExecutorWeakReference = new WeakReference<>( tileRenderPoolExecutor );
    }

    public TileRenderPoolExecutor getTileRenderPoolExecutor() {
      if( mTileRenderPoolExecutorWeakReference != null ) {
        return mTileRenderPoolExecutorWeakReference.get();
      }
      return null;
    }

    public void setTileCanvasViewGroup( TileCanvasViewGroup tileCanvasViewGroup ) {
      mTileCanvasViewGroupWeakReference = new WeakReference<>( tileCanvasViewGroup );
    }

    public TileCanvasViewGroup getTileCanvasViewGroup() {
      if( mTileCanvasViewGroupWeakReference != null ) {
        return mTileCanvasViewGroupWeakReference.get();
      }
      return null;
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

    public void cancel() {
      mCancelled = true;
    }

    public boolean renderTile() {
      if( mCancelled ) {
        return false;
      }
      Process.setThreadPriority( Process.THREAD_PRIORITY_BACKGROUND );
      final Thread thread = Thread.currentThread();
      if( thread.isInterrupted() ) {
        return false;
      }
      TileCanvasViewGroup tileCanvasViewGroup = getTileCanvasViewGroup();
      if( tileCanvasViewGroup == null ) {
        return false;
      }
      if( tileCanvasViewGroup.getRenderIsCancelled() ) {
        return false;
      }
      Tile tile = getTile();
      if( tile == null ) {
        return false;
      }
      try {
        tileCanvasViewGroup.generateTileBitmap( tile );
      } catch( InterruptedIOException e ) {
        Thread.currentThread().interrupt();
        return false;
      } catch( Exception e ) {
        return false;
      }
      if( mCancelled || tile.getBitmap() == null || thread.isInterrupted() || tileCanvasViewGroup.getRenderIsCancelled() ) {
        tile.destroy( true );
        return false;
      }
      tileCanvasViewGroup.addTileToCurrentTileCanvasView( tile );
      return true;
    }

    @Override
    public void run() {
      boolean rendered = renderTile();
      if( rendered ) {
        markComplete();
      }
    }
  }
}
