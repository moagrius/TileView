package com.qozix.tileview.tiles;

import android.content.Context;
import android.os.*;
import android.os.Process;
import android.support.annotation.NonNull;

import com.qozix.tileview.graphics.BitmapProvider;

import java.lang.ref.WeakReference;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Dunn, 3/10/16.
 */
class TileRenderRunnable implements Runnable, Future<Void> {

  private WeakReference<TileCanvasViewGroup> mTileCanvasViewGroupWeakReference;
  private WeakReference<Tile> mTileWeakReference;
  private WeakReference<Handler> mHandlerWeakReference;
  private WeakReference<Context> mContextWeakReference;
  private WeakReference<BitmapProvider> mBitmapProviderWeakReference;

  private volatile boolean mCancelled = false;
  private volatile boolean mComplete = false;

  private Throwable mThrowable;

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

  public Throwable getThrowable() {
    return mThrowable;
  }

  public TileRenderHandler.Status renderTile() {
    if( mCancelled ) {
      return TileRenderHandler.Status.INCOMPLETE;
    }
    android.os.Process.setThreadPriority( Process.THREAD_PRIORITY_BACKGROUND );
    final Thread thread = Thread.currentThread();
    if( thread.isInterrupted() ) {
      return TileRenderHandler.Status.INCOMPLETE;
    }
    Tile tile = getTile();
    if( tile == null ) {
      return TileRenderHandler.Status.INCOMPLETE;
    }
    Context context = getContext();
    if( context == null ) {
      return TileRenderHandler.Status.INCOMPLETE;
    }
    BitmapProvider bitmapProvider = getBitmapProvider();
    if( bitmapProvider == null ) {
      return TileRenderHandler.Status.INCOMPLETE;
    }
    try {
      tile.generateBitmap( context, bitmapProvider );
    } catch( Throwable throwable ) {
      mThrowable = throwable;
      return TileRenderHandler.Status.ERROR;
    }
    if( mCancelled || tile.getBitmap() == null || thread.isInterrupted() ) {
      tile.destroy( true );
      return TileRenderHandler.Status.INCOMPLETE;
    }
    return TileRenderHandler.Status.COMPLETE;
  }

  @Override
  public void run() {
    TileRenderHandler.Status status = renderTile();
    Handler handler = getHandler();
    if( handler != null ) {
      Message message = handler.obtainMessage( status.getMessageCode(), this );
      message.sendToTarget();
    }
    if( status == TileRenderHandler.Status.COMPLETE ) {
      mComplete = true;
    }
  }
}
