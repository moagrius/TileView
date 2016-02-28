package com.qozix.tileview.tiles;

import android.os.Process;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.InterruptedIOException;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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

  public TileRenderPoolExecutor() {
    super(
      CORE_POOL_SIZE,
      MAXIMUM_POOL_SIZE,
      KEEP_ALIVE_TIME,
      KEEP_ALIVE_TIME_UNIT,
      new LinkedBlockingDeque<Runnable>()
    );
  }

  public void queue( TileCanvasViewGroup tileCanvasViewGroup, Set<Tile> renderList ) {
    synchronized( this ) {
      List<String> positions = new LinkedList<>();
      for( Tile tile : renderList ) {
        positions.add( tile.getColumn() + ":" + tile.getRow() );
      }
      Log.d( "DEBUG", "visible tiles: " + TextUtils.join( ",", positions ) );
      for( Runnable runnable : getQueue() ) {
        if( runnable instanceof TileRenderRunnable ) {
          TileRenderRunnable tileRenderRunnable = (TileRenderRunnable) runnable;
          Tile tile = tileRenderRunnable.getTile();
          if( tile == null ) {
            Log.d( "DEBUG", "skipping null tile" );
            continue;
          }
          if( tileRenderRunnable.isDone() ){
            Log.d( "DEBUG", "tile at " + tile.getColumn() + ", " + tile.getRow() + " is in queue but is done already" );
            continue;
          }
          Log.d( "DEBUG", "tile at " + tile.getColumn() + ", " + tile.getRow() + " is in queue" );
          if( renderList.contains( tile ) ) {
            Log.d( "DEBUG", "removing tile at " + tile.getColumn() + ", " + tile.getRow() );
            renderList.remove( tile );
          } else {
            Log.d( "DEBUG", "tile is not in current visible viewport, cancelling tile at " + tile.getColumn() + ", " + tile.getRow() );
            tileRenderRunnable.cancel( true );
            remove( tileRenderRunnable );
          }
        }
      }
      if( renderList.size() > 0 ) {
        mTileCanvasViewGroupWeakReference = new WeakReference<>( tileCanvasViewGroup );
        tileCanvasViewGroup.onRenderTaskPreExecute();
        for( Tile tile : renderList ) {
          if( isShutdownOrTerminating() ) {
            Log.d( "DEBUG", "skipping tile at " + tile.getColumn() + ", " + tile.getRow() );
            return;
          }
          TileRenderRunnable runnable = new TileRenderRunnable();
          runnable.setTile( tile );
          runnable.setTileCanvasViewGroup( tileCanvasViewGroup );
          execute( runnable );
        }
      }
    }
  }

  public void cancel(){
    if( getQueue().size() > 0 || getActiveCount() > 0 ) {
      Log.d( "DEBUG", "queue 0, active count 0, dispatch render cancelled event" );
      TileCanvasViewGroup tileCanvasViewGroup = mTileCanvasViewGroupWeakReference.get();
      if( tileCanvasViewGroup != null ) {
        tileCanvasViewGroup.onRenderTaskCancelled();
        Log.d( "DEBUG", "render cancelled event dispatched" );
      }
    }
    getQueue().clear();
  }

  public boolean isShutdownOrTerminating() {
    return isShutdown() || isTerminating() || isTerminated();
  }

  @Override
  protected void afterExecute( Runnable runnable, Throwable throwable ) {
    synchronized( this ) {
      super.afterExecute( runnable, throwable );
      Log.d( "DEBUG", "queue size=" + getQueue().size() );
      if( getQueue().size() == 0 && getActiveCount() == 1 ) {
        Log.d( "DEBUG", "last task done" );
        TileCanvasViewGroup tileCanvasViewGroup = mTileCanvasViewGroupWeakReference.get();
        if( tileCanvasViewGroup != null ) {
          tileCanvasViewGroup.onRenderTaskPostExecute();
          Log.d( "DEBUG", "afterExecute should send onRenderTaskPostExecute" );
        }
      }
    }
  }

  private static class TileRenderRunnable implements Runnable, Future<Void> {

    private WeakReference<TileCanvasViewGroup> mTileCanvasViewGroupWeakReference;
    private WeakReference<Tile> mTileWeakReference;

    private volatile boolean mCancelled = false;
    private volatile boolean mComplete = false;
    // TODO: mExcepted

    public TileRenderRunnable() {

    }

    public Void get(){
      return null;
    }

    public Void get(long timeout, @NonNull TimeUnit unit){
      return null;
    }

    public boolean cancel( boolean mayInterrupt ) {
      if( mayInterrupt ){
        Thread.currentThread().interrupt();
      }
      boolean isAlreadyCancelled = mCancelled;
      mCancelled = true;
      return !isAlreadyCancelled;
    }

    public void markComplete() {
      mComplete = true;
    }

    public boolean isComplete(){
      return mComplete;
    }

    public boolean isCancelled(){
      return mCancelled;
    }

    public boolean isDone(){
      return mCancelled || mComplete;
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
        //tile.destroy( true );
        return false;
      }
      tileCanvasViewGroup.addTileToCurrentTileCanvasView( tile );
      return true;
    }

    @Override
    public void run() {
      if(renderTile()) {
        markComplete();
      }
    }
  }
}
