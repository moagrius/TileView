package com.qozix.tileview.tiles;

import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TileRenderPoolExecutor {
  // Sets the amount of time an idle thread will wait for a task before terminating
  private static final int KEEP_ALIVE_TIME = 5;
  private static final TimeUnit KEEP_ALIVE_TIME_UNIT;

  private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
  private static final int MAXIMUM_POOL_SIZE = Runtime.getRuntime().availableProcessors();

  private final ThreadPoolExecutor mExecutor;
  private final BlockingQueue<Runnable> mQueue;
  private final BlockingQueue<Future> mFutureList;

  private static TileRenderPoolExecutor sInstance = null;

  static {
    KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    sInstance = new TileRenderPoolExecutor();
  }

  public TileRenderPoolExecutor() {
    mQueue = new LinkedBlockingDeque<>();
    mExecutor = new ThreadPoolExecutor( CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mQueue );
    mFutureList = new LinkedBlockingDeque<>();
  }

  public static TileRenderPoolExecutor getsInstance() {
    return sInstance;
  }

  public void cancel() {
    synchronized ( this ) {
      for ( Future f : mFutureList ) {
        if( !f.isDone() ){
          f.cancel(true);
        }
      }
      mQueue.clear();
      mFutureList.clear();
    }
  }

  public void clear(){
    synchronized ( this ) {
      mQueue.clear();
      mFutureList.clear();
    }
  }

  public void queue(TileCanvasViewGroup tileCanvasViewGroup, LinkedList<Tile> tiles) {
    if( tiles != null && tiles.size() > 0) {
      tileCanvasViewGroup.onRenderTaskPreExecute();
      int size = tiles.size();
      for( int i=0 ; i<size ; i++ ) {
        mFutureList.add(mExecutor.submit( new TileRenderRunnable(tileCanvasViewGroup, tiles.get(i), i == size - 1 ) ) );
      }
    }
  }

  static class TileRenderRunnable implements Runnable {
    private final WeakReference<TileCanvasViewGroup> mTileCanvasViewGroup;
    private final WeakReference<Tile> mTile;
    private final boolean mIsLast;

    public TileRenderRunnable( TileCanvasViewGroup tileCanvasViewGroup, Tile tile, boolean last ) {
      this.mTileCanvasViewGroup = new WeakReference<>(tileCanvasViewGroup);
      this.mTile = new WeakReference<>( tile );
      this.mIsLast = last;
    }

    @Override
    public void run() {
      android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
      final Thread thread = Thread.currentThread();

      TileCanvasViewGroup tileCanvasViewGroup = mTileCanvasViewGroup.get();
      Tile tile = mTile.get();

      if( tileCanvasViewGroup != null ){
        if( thread.isInterrupted() ){
          tileCanvasViewGroup.onRenderTaskCancelled();
          return;
        }

        if( !tileCanvasViewGroup.getRenderIsCancelled() && tile != null ){
          tileCanvasViewGroup.generateTileBitmap( tile );
          if (thread.isInterrupted()){
            tileCanvasViewGroup.onRenderTaskCancelled();
            return;
          }

          tileCanvasViewGroup.addTileToCurrentTileCanvasView( tile );

          if( mIsLast ){
            if( thread.isInterrupted() ){
              tileCanvasViewGroup.onRenderTaskCancelled();
              return;
            }

            tileCanvasViewGroup.onRenderTaskPostExecute();
          }
        }
      }
    }
  }
}
