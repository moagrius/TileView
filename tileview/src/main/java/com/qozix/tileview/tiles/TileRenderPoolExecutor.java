package com.qozix.tileview.tiles;

import android.os.Process;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TileRenderPoolExecutor {
  // Sets the amount of time an idle thread will wait for a task before terminating
  private static final int KEEP_ALIVE_TIME = 1;
  private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

  private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
  private static final int MAXIMUM_POOL_SIZE = Runtime.getRuntime().availableProcessors();

  private final CustomThreadPoolExecutor mExecutor;
  private final BlockingQueue<Runnable> mQueue;
  private final BlockingQueue<Future> mFutureList;
  private WeakReference<TileCanvasViewGroup> mViewGroup;

  public TileRenderPoolExecutor() {
    mQueue = new LinkedBlockingDeque<>();
    mExecutor = new CustomThreadPoolExecutor( CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mQueue );
    mFutureList = new LinkedBlockingDeque<>();
  }

  public void cancel() {
    synchronized ( this ) {
      if( ( mViewGroup != null && mViewGroup.get() != null ) && ( mQueue.size()>0 || mExecutor.getActiveCount() > 0 ) ){
        mViewGroup.get().onRenderTaskCancelled();
      }

      for ( Future f : mFutureList ) {
        if( !f.isDone() ){
          f.cancel( true );
        }
      }
      mQueue.clear();
      mFutureList.clear();
    }
  }

  public void shutDown(){
    cancel();
    mExecutor.shutdownNow();
  }

  public void queue( TileCanvasViewGroup viewGroup, LinkedList<Tile> tiles ) {
    if( tiles != null && tiles.size() > 0 ) {
      mViewGroup = new WeakReference<>( viewGroup );
      viewGroup.onRenderTaskPreExecute();
      for ( Tile tile : tiles ){
        if( mExecutor.isShutdown() || mExecutor.isTerminating() || mExecutor.isTerminated() ){
          return;
        }
        mFutureList.add( mExecutor.submit( new TileRenderRunnable( viewGroup, tile ) ) );
      }
    }
  }

  static class TileRenderRunnable implements Runnable {
    private final WeakReference<TileCanvasViewGroup> mTileCanvasViewGroup;
    private final WeakReference<Tile> mTile;

    public TileRenderRunnable( TileCanvasViewGroup viewGroup, Tile tile ) {
      mTileCanvasViewGroup = new WeakReference<>( viewGroup );
      mTile = new WeakReference<>( tile );
    }

    @Override
    public void run() {
      Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND );
      final Thread thread = Thread.currentThread();

      TileCanvasViewGroup viewGroup = mTileCanvasViewGroup.get();
      Tile tile = mTile.get();

      if( viewGroup != null ){
        if( thread.isInterrupted() ){
          return;
        }

        if( !viewGroup.getRenderIsCancelled() && tile != null ){
          viewGroup.generateTileBitmap(tile);

          if( tile.getBitmap() == null || thread.isInterrupted() || viewGroup.getRenderIsCancelled() ){
            return;
          }

          viewGroup.addTileToCurrentTileCanvasView( tile );
        }
      }
    }
  }

  private class CustomThreadPoolExecutor extends ThreadPoolExecutor{
    public CustomThreadPoolExecutor( int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue ) {
      super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue );
    }

    @Override
    protected void afterExecute( Runnable r, Throwable t ) {
      super.afterExecute( r, t );
      //getActiveCount() == 1 to make sure it is going to execute only for the last thread to run
      if( mQueue != null && mQueue.size() == 0 && mViewGroup != null && mViewGroup.get() != null && getActiveCount() == 1 ) {
        mViewGroup.get().onRenderTaskPostExecute();
        mFutureList.clear();
      }
    }
  }
}
