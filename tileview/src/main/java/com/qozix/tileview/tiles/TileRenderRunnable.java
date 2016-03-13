package com.qozix.tileview.tiles;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Process;

import com.qozix.tileview.graphics.BitmapProvider;

import java.lang.ref.WeakReference;

/**
 * @author Mike Dunn, 3/10/16.
 */
class TileRenderRunnable implements Runnable {

  private WeakReference<Tile> mTileWeakReference;
  private WeakReference<Handler> mHandlerWeakReference;
  private WeakReference<Context> mContextWeakReference;
  private WeakReference<BitmapProvider> mBitmapProviderWeakReference;

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

  public void setContext( Context context ) {
    mContextWeakReference = new WeakReference<>( context );
  }

  public void setBitmapProvider( BitmapProvider bitmapProvider ) {
    mBitmapProviderWeakReference = new WeakReference<>( bitmapProvider );
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
    if( mThread.isInterrupted() ) {
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
    if( mCancelled || tile.getBitmap() == null || mThread.isInterrupted() ) {
      tile.destroy( true );
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
    Handler handler = getHandler();
    if( handler != null ) {
      Message message = handler.obtainMessage( status.getMessageCode(), this );
      message.sendToTarget();
    }
  }
}
