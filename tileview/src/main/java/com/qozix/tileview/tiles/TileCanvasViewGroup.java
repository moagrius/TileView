package com.qozix.tileview.tiles;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.qozix.tileview.detail.DetailLevel;
import com.qozix.tileview.graphics.BitmapProvider;
import com.qozix.tileview.graphics.BitmapProviderAssets;
import com.qozix.tileview.widgets.ScalingLayout;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;

public class TileCanvasViewGroup extends ScalingLayout implements TileCanvasView.TileCanvasDrawListener {

  private static final int RENDER_FLAG = 1;

  private static final int DEFAULT_RENDER_BUFFER = 250;
  private static final int DEFAULT_TRANSITION_DURATION = 200;

  private LinkedList<Tile> mTilesScheduledToRender = new LinkedList<Tile>();
  private LinkedList<Tile> mTilesAlreadyRendered = new LinkedList<Tile>();

  private BitmapProvider mBitmapProvider;
  private HashMap<Float, TileCanvasView> mTileCanvasViewHashMap = new HashMap<Float, TileCanvasView>();

  private TileRenderTask mLastRunTileRenderTask;

  private DetailLevel mDetailLevelToRender;
  private DetailLevel mLastRenderedDetailLevel;
  private TileCanvasView mCurrentTileCanvasView;

  private boolean mRenderIsCancelled = false;
  private boolean mRenderIsSuppressed = false;
  private boolean mIsRendering = false;

  private boolean mShouldRecycleBitmaps = true;

  private boolean mTransitionsEnabled = true;
  private int mTransitionDuration = DEFAULT_TRANSITION_DURATION;

  private TileRenderHandler mTileRenderHandler;
  private TileRenderListener mTileRenderListener;

  private int mRenderBuffer = DEFAULT_RENDER_BUFFER;

  public TileCanvasViewGroup( Context context ) {
    super( context );
    setWillNotDraw( false );
    mTileRenderHandler = new TileRenderHandler( this );
  }

  public boolean getTransitionsEnabled() {
    return mTransitionsEnabled;
  }

  public void setTransitionsEnabled( boolean enabled ) {
    mTransitionsEnabled = enabled;
  }

  public int getTransitionDuration() {
    return mTransitionDuration;
  }

  public void setTransitionDuration( int duration ) {
    mTransitionDuration = duration;
  }

  public BitmapProvider getBitmapProvider(){
    if( mBitmapProvider == null ) {
      mBitmapProvider = new BitmapProviderAssets();
    }
    return mBitmapProvider;
  }

  public void setBitmapProvider( BitmapProvider bitmapProvider ) {
    mBitmapProvider = bitmapProvider;
  }

  public void setTileRenderListener( TileRenderListener tileRenderListener ) {
    mTileRenderListener = tileRenderListener;
  }

  public int getRenderBuffer() {
    return mRenderBuffer;
  }

  public void setRenderBuffer( int renderBuffer ) {
    mRenderBuffer = renderBuffer;
  }

  public boolean getShouldRecycleBitmaps() {
    return mShouldRecycleBitmaps;
  }

  public void setShouldRecycleBitmaps( boolean shouldRecycleBitmaps ) {
    mShouldRecycleBitmaps = shouldRecycleBitmaps;
  }


  /**
   * The layout dimensions supplied to this ViewGroup will be exactly as large as the scaled
   * width and height of the containing ZoomPanLayout (or TileView).  However, when the canvas
   * is scaled, it's clip area is also scaled - offset this by providing dimensions scaled as
   * large as the smallest size the TileCanvasView might be.
   */

  public void requestRender() {
    mRenderIsCancelled = false;
    mRenderIsSuppressed = false;
    if( mDetailLevelToRender == null ) {
      return;
    }
    if( !mTileRenderHandler.hasMessages( RENDER_FLAG ) ) {
      mTileRenderHandler.sendEmptyMessageDelayed( RENDER_FLAG, mRenderBuffer );
    }
  }

  /**
   * Prevent new render tasks from starting, attempts to interrupt ongoing tasks, and will
   * prevent queued tiles from begin decoded or rendered.
   */
  public void cancelRender() {
    mRenderIsCancelled = true;
    if( mLastRunTileRenderTask != null && mLastRunTileRenderTask.getStatus() != AsyncTask.Status.FINISHED ) {
      mLastRunTileRenderTask.cancel( true );
    }
    mLastRunTileRenderTask = null;
  }

  /**
   * Prevent new render tasks from starting, but does not cancel any ongoing operations.
   */
  public void suppressRender() {
    mRenderIsSuppressed = true;
  }

  public void updateTileSet( DetailLevel detailLevel ) {
    mDetailLevelToRender = detailLevel;
    if( mDetailLevelToRender == null ) {
      return;
    }
    if( mDetailLevelToRender.equals( mLastRenderedDetailLevel ) ) {
      return;
    }
    mLastRenderedDetailLevel = mDetailLevelToRender;
    mCurrentTileCanvasView = getCurrentTileCanvasView();
    mCurrentTileCanvasView.bringToFront();
    cancelRender();
    requestRender();
  }

  public boolean getIsRendering() {
    return mIsRendering;
  }

  public void clear() {
    suppressRender();
    cancelRender();
    for( Tile tile : mTilesScheduledToRender ) {
      tile.destroy( mShouldRecycleBitmaps );
    }
    mTilesScheduledToRender.clear();
    for( Tile tile : mTilesAlreadyRendered ) {
      tile.destroy( mShouldRecycleBitmaps );
    }
    mTilesAlreadyRendered.clear();
  }

  private float getCurrentDetailLevelScale() {
    if( mDetailLevelToRender != null ) {
      return mDetailLevelToRender.getScale();
    }
    return 1;
  }

  private TileCanvasView getCurrentTileCanvasView() {
    float levelScale = getCurrentDetailLevelScale();
    if( mTileCanvasViewHashMap.containsKey( levelScale ) ) {
      return mTileCanvasViewHashMap.get( levelScale );
    }
    TileCanvasView tileGroup = new TileCanvasView( getContext() );
    tileGroup.setTileCanvasDrawListener( this );
    tileGroup.setScale( 1 / levelScale );
    mTileCanvasViewHashMap.put( levelScale, tileGroup );
    addView( tileGroup );
    return tileGroup;
  }

  void renderTiles() {
    if( !mRenderIsCancelled && !mRenderIsSuppressed && mDetailLevelToRender != null ) {
      beginRenderTask();
    }
  }

  private void beginRenderTask() {
    boolean changed = mDetailLevelToRender.computeCurrentState();
    if( !changed ) {
      return;
    }
    mTilesScheduledToRender = mDetailLevelToRender.getVisibleTilesFromLastViewportComputation();
    if( mLastRunTileRenderTask != null && mLastRunTileRenderTask.getStatus() != AsyncTask.Status.FINISHED ) {
      mLastRunTileRenderTask.cancel( true );
    }
    mLastRunTileRenderTask = new TileRenderTask( this );
    mLastRunTileRenderTask.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR );
  }

  private void cleanup() {
    LinkedList<Tile> condemned = new LinkedList<Tile>( mTilesAlreadyRendered );
    condemned.removeAll( mTilesScheduledToRender );
    for( Tile tile : condemned ) {
      tile.destroy( mShouldRecycleBitmaps );
    }
    mTilesAlreadyRendered.removeAll( condemned );
    mCurrentTileCanvasView.invalidate();
    for( TileCanvasView tileGroup : mTileCanvasViewHashMap.values() ) {
      if( mCurrentTileCanvasView != tileGroup ) {
        tileGroup.clearTiles( mShouldRecycleBitmaps );
      }
    }
    invalidate();
  }

  void onRenderTaskPreExecute() {
    mIsRendering = true;
    if( mTileRenderListener != null ) {
      mTileRenderListener.onRenderStart();
    }
  }

  void onRenderTaskCancelled() {
    if( mTileRenderListener != null ) {
      mTileRenderListener.onRenderCancelled();
    }
    mIsRendering = false;
  }

  void onRenderTaskPostExecute() {
    mIsRendering = false;
    if( !mTransitionsEnabled ) {
      cleanup();
    }
    if( mTileRenderListener != null ) {
      mTileRenderListener.onRenderComplete();
    }
    invalidate();
    requestRender();
  }

  LinkedList<Tile> getRenderList() {
    return (LinkedList<Tile>) mTilesScheduledToRender.clone();
  }

  void generateTileBitmap( Tile tile ) {
    tile.generateBitmap( getContext(), getBitmapProvider() );
  }

  void addTileToCurrentTileCanvasView( Tile tile ) {
    if( mTilesAlreadyRendered.contains( tile ) ) {
      return;
    }
    tile.setTransitionsEnabled( mTransitionsEnabled );
    tile.setTransitionDuration( mTransitionDuration );
    tile.stampTime();
    mTilesAlreadyRendered.add( tile );
    mCurrentTileCanvasView.addTile( tile );
  }

  boolean getRenderIsCancelled() {
    return mRenderIsCancelled;
  }

  @Override
  public void onDrawComplete( TileCanvasView tileCanvasView ) {
    if( mTransitionsEnabled && tileCanvasView == mCurrentTileCanvasView ) {
      cleanup();
    }
  }

  @Override
  public void onDrawPending( TileCanvasView tileCanvasView ) {
    invalidate();
  }

  public void destroy(){
    clear();
    for( TileCanvasView tileGroup : mTileCanvasViewHashMap.values() ) {
      tileGroup.clearTiles( mShouldRecycleBitmaps );
    }
    mTileCanvasViewHashMap.clear();
    if( !mTileRenderHandler.hasMessages( RENDER_FLAG ) ) {
      mTileRenderHandler.removeMessages( RENDER_FLAG );
    }
  }

  private static class TileRenderHandler extends Handler {

    private final WeakReference<TileCanvasViewGroup> mTileManagerWeakReference;

    public TileRenderHandler( TileCanvasViewGroup tileCanvasViewGroup ) {
      super();
      mTileManagerWeakReference = new WeakReference<TileCanvasViewGroup>( tileCanvasViewGroup );
    }

    @Override
    public final void handleMessage( Message message ) {
      final TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
      if( tileCanvasViewGroup != null ) {
        tileCanvasViewGroup.renderTiles();
      }
    }
  }

  /**
   * Interface definition for callbacks to be invoked after render operations.
   */
  public interface TileRenderListener {
    void onRenderStart();
    void onRenderCancelled();
    void onRenderComplete();
  }

  static class TileRenderTask extends AsyncTask<Void, Tile, Void> {

    private final WeakReference<TileCanvasViewGroup> mTileManagerWeakReference;

    TileRenderTask( TileCanvasViewGroup tileCanvasViewGroup ) {
      super();
      mTileManagerWeakReference = new WeakReference<TileCanvasViewGroup>( tileCanvasViewGroup );
    }

    @Override
    protected void onPreExecute() {
      final TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
      if( tileCanvasViewGroup != null ) {
        tileCanvasViewGroup.onRenderTaskPreExecute();
      }
    }

    /**
     * As of 10/03/15, lint is _incorrectly_ indicating that we can't access member
     * variables from a worker thread (this thread).
     * https://code.google.com/p/android/issues/detail?id=175397
     * Until this is corrected, use @SuppressWarnings
     *
     * @param params noop
     * @return null
     */
    @SuppressWarnings("all")
    @Override
    protected Void doInBackground( Void... params ) {
      TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
      if( tileCanvasViewGroup != null ) {
        LinkedList<Tile> renderList = tileCanvasViewGroup.getRenderList();
        for( Tile tile : renderList ) {
          if( !isCancelled() ) {
            tileCanvasViewGroup = mTileManagerWeakReference.get();
            if( tileCanvasViewGroup != null && !tileCanvasViewGroup.getRenderIsCancelled() ) {
              tileCanvasViewGroup.generateTileBitmap( tile );
              publishProgress( tile );
            }
          }
        }
      }
      return null;
    }

    @Override
    protected void onProgressUpdate( Tile... params ) {
      if( !isCancelled() ) {
        TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
        if( tileCanvasViewGroup != null && !tileCanvasViewGroup.getRenderIsCancelled() ) {
          Tile tile = params[0];
          tileCanvasViewGroup.addTileToCurrentTileCanvasView( tile );
        }
      }
    }

    @Override
    protected void onPostExecute( Void param ) {
      TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
      if( tileCanvasViewGroup != null ) {
        tileCanvasViewGroup.onRenderTaskPostExecute();
      }
    }

    @Override
    protected void onCancelled() {
      TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
      if( tileCanvasViewGroup != null ) {
        tileCanvasViewGroup.onRenderTaskCancelled();
      }
    }

  }
}
