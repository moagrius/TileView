package com.qozix.tileview.tiles;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;

import com.qozix.tileview.graphics.BitmapProvider;

import java.lang.ref.WeakReference;
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

  private Handler mHandler;

  public TileRenderPoolExecutor() {
    super(
      CORE_POOL_SIZE,
      MAXIMUM_POOL_SIZE,
      KEEP_ALIVE_TIME,
      KEEP_ALIVE_TIME_UNIT,
      new LinkedBlockingDeque<Runnable>()
    );
    mHandler = new TileRenderHandler();
  }

  public void queue( TileCanvasViewGroup tileCanvasViewGroup, Set<Tile> renderSet ) {
    mTileCanvasViewGroupWeakReference = new WeakReference<>( tileCanvasViewGroup );
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
      runnable.setTileCanvasViewGroup( tileCanvasViewGroup );
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

  @Override
  public void shutdown() {
    super.shutdown();
    broadcastCancel();
  }

  @Override
  @NonNull
  public List<Runnable> shutdownNow() {
    List<Runnable> tasks = super.shutdownNow();
    broadcastCancel();
    return tasks;
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

  public static class TileRenderHandler extends Handler {

    public static final int RENDER_INCOMPLETE = 0;
    public static final int RENDER_COMPLETE = 1;

    public TileRenderHandler() {
      this( Looper.getMainLooper() );
    }

    public TileRenderHandler( Looper looper ) {
      super( looper );
    }

    @Override
    public void handleMessage( Message message ) {
      switch( message.what ) {
        case RENDER_COMPLETE:
          TileRenderRunnable tileRenderRunnable = (TileRenderRunnable) message.obj;
          TileCanvasViewGroup tileCanvasViewGroup = tileRenderRunnable.getTileCanvasViewGroup();
          if( tileCanvasViewGroup != null ) {
            Tile tile = tileRenderRunnable.getTile();
            if( tile != null ) {
              tileCanvasViewGroup.addTileToCurrentTileCanvasView( tile );
            }
          }
          break;
        case RENDER_INCOMPLETE:
          Log.w( TileRenderPoolExecutor.class.getSimpleName(), "Tile was queued but was not rendered" );
          break;
      }
    }
  }

  private static class TileRenderRunnable implements Runnable, Future<Void> {

    private WeakReference<TileCanvasViewGroup> mTileCanvasViewGroupWeakReference;
    private WeakReference<Tile> mTileWeakReference;
    private WeakReference<Handler> mHandlerWeakReference;
    private WeakReference<Context> mContextWeakReference;
    private WeakReference<BitmapProvider> mBitmapProviderWeakReference;

    private volatile boolean mCancelled = false;
    private volatile boolean mComplete = false;

    @Override
    public Void get() {
      return null;
    }

    @Override
    public Void get( long timeout, @NonNull TimeUnit unit ) {
      return null;
    }

    @Override
    public boolean cancel( boolean mayInterrupt ) {
      if( mayInterrupt ) {
        Thread.currentThread().interrupt();
      }
      boolean cancelled = mCancelled;
      mCancelled = true;
      return !cancelled;
    }

    public boolean isCancelled() {
      return mCancelled;
    }

    public boolean isDone() {
      return mComplete;
    }

    public void setHandler( Handler handler ) {
      mHandlerWeakReference = new WeakReference<>( handler );
    }

    public Handler getHandler() {
      if( mHandlerWeakReference == null ) {
        return null;
      }
      return mHandlerWeakReference.get();
    }

    public void setTileCanvasViewGroup( TileCanvasViewGroup tileCanvasViewGroup ) {
      mTileCanvasViewGroupWeakReference = new WeakReference<>( tileCanvasViewGroup );
      mContextWeakReference = new WeakReference<>( tileCanvasViewGroup.getContext() );
      mBitmapProviderWeakReference = new WeakReference<>( tileCanvasViewGroup.getBitmapProvider() );
    }

    public TileCanvasViewGroup getTileCanvasViewGroup() {
      if( mTileCanvasViewGroupWeakReference != null ) {
        return mTileCanvasViewGroupWeakReference.get();
      }
      return null;
    }

    public Context getContext() {
      if( mContextWeakReference == null ) {
        return null;
      }
      return mContextWeakReference.get();
    }

    public BitmapProvider getBitmapProvider() {
      if( mBitmapProviderWeakReference == null ) {
        return null;
      }
      return mBitmapProviderWeakReference.get();
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

    // TODO: somewhere check OOM and call .cleanup, in case of constant slow dragging
    // TODO: TCV.clearTiles throw concurrent modification exception on Set
    public boolean renderTile() {
      if( mCancelled ) {
        return false;
      }
      Process.setThreadPriority( Process.THREAD_PRIORITY_BACKGROUND );
      final Thread thread = Thread.currentThread();
      if( thread.isInterrupted() ) {
        return false;
      }
      Tile tile = getTile();
      if( tile == null ) {
        return false;
      }
      Context context = getContext();
      if( context == null ) {
        return false;
      }
      BitmapProvider bitmapProvider = getBitmapProvider();
      if( bitmapProvider == null ) {
        return false;
      }
      try {
        tile.generateBitmap( context, bitmapProvider );
      } catch( Exception e ) {
        return false;
      }
      if( mCancelled || tile.getBitmap() == null || thread.isInterrupted() ) {
        tile.destroy( true );
        return false;
      }
      return true;
    }

    @Override
    public void run() {
      boolean rendered = renderTile();
      Handler handler = getHandler();
      if( handler != null ) {
        int status = rendered
          ? TileRenderHandler.RENDER_COMPLETE
          : TileRenderHandler.RENDER_INCOMPLETE;
        Message message = handler.obtainMessage( status, this );
        message.sendToTarget();
      }
      if( rendered ) {
        mComplete = true;
      }
    }
  }
}
