package com.qozix.tileview.tiles;

import android.os.Process;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RunnableFuture;
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
  
  private HashMap<Future, Runnable> mFutureRunnableHashMap = new HashMap<>();

  public TileRenderPoolExecutor() {
    mQueue = new LinkedBlockingDeque<>();
    mExecutor = new CustomThreadPoolExecutor( CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mQueue );
    mFutureList = new LinkedBlockingDeque<>();
  }

  public void cancel() {
    synchronized ( this ) {
      if( isViewGroupValid() && ( mQueue.size()>0 || mExecutor.getActiveCount() > 0 ) ){
        mViewGroup.get().onRenderTaskCancelled();
      }

      for ( Future f : mFutureList ) {
        if( !f.isDone() ){
          try {
            if( f instanceof FutureTaskWithRunnableReference ) {
              FutureTaskWithRunnableReference futureTaskWithRunnableReference = (FutureTaskWithRunnableReference) f;
              Runnable runnable = futureTaskWithRunnableReference.getRunnable();
              if( runnable instanceof TileRenderRunnable ) {
                TileRenderRunnable tileRenderRunnable = (TileRenderRunnable) runnable;
                tileRenderRunnable.mCancelled = true;
              }
            }
            f.cancel( true );  // TODO: this is the money
          } catch( Exception e ) {
            Log.d( "DEBUG", "Future.cancel" );
          }
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

  private boolean isViewGroupValid(){
    return mViewGroup != null && mViewGroup.get() != null;
  }

  // TODO: http://www.javaspecialists.eu/archive/Issue056.html

  static class TileRenderRunnable implements Runnable {

    private boolean mCancelled = false;

    private final WeakReference<TileCanvasViewGroup> mTileCanvasViewGroup;
    private final WeakReference<Tile> mTile;

    public TileRenderRunnable( TileCanvasViewGroup viewGroup, Tile tile ) {
      mTileCanvasViewGroup = new WeakReference<>( viewGroup );
      mTile = new WeakReference<>( tile );
    }

    public void cancel(){
      mCancelled = true;
    }

    @Override
    public void run() {

      if( mCancelled ) {
        return;
      }

      Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY );

      final Thread thread = Thread.currentThread();

      TileCanvasViewGroup viewGroup = mTileCanvasViewGroup.get();
      Tile tile = mTile.get();

      if( viewGroup != null ){

        if( thread.isInterrupted() ){
          return;
        }

        if( !viewGroup.getRenderIsCancelled() && tile != null ){

          try {
            viewGroup.generateTileBitmap( tile );

          } catch( Exception e ) {
            // catch anything that happened during decode, which is a possibility because there may be a variety of long running operations occuring in the decode method, e.g., http requests
            Log.d( "DEBUG", "TileRenderRunnable.run" );
            return;
          }
          if( mCancelled || tile.getBitmap() == null || thread.isInterrupted() || viewGroup.getRenderIsCancelled() ){
            tile.destroy( true );
            return;
          }

          viewGroup.addTileToCurrentTileCanvasView( tile );
        }
      }
    }
  }

  private static class FutureTaskWithRunnableReference<T> extends FutureTask<T> {
    public WeakReference<Runnable> mRunnableWeakReference;
    public FutureTaskWithRunnableReference(Runnable runnable, T value ) {
      super( runnable, value );
      mRunnableWeakReference = new WeakReference<>( runnable );
    }
    public Runnable getRunnable(){
      return mRunnableWeakReference.get();
    }
  }

  private class CustomThreadPoolExecutor extends ThreadPoolExecutor{
    public CustomThreadPoolExecutor( int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue ) {
      super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue );
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
      return new FutureTaskWithRunnableReference<T>(runnable, value);
    }

    @Override
    protected void afterExecute( Runnable r, Throwable t ) {
      super.afterExecute( r, t );
      //getActiveCount() == 1 to make sure it is going to execute only for the last thread to run
      if( mQueue != null && mQueue.size() == 0 && isViewGroupValid() && getActiveCount() == 1 ) {
        mViewGroup.get().onRenderTaskPostExecute();
        mFutureList.clear();
      }
    }
  }
}
