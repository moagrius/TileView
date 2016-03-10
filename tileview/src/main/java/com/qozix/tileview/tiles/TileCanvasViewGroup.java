package com.qozix.tileview.tiles;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.qozix.tileview.detail.DetailLevel;
import com.qozix.tileview.graphics.BitmapProvider;
import com.qozix.tileview.graphics.BitmapProviderAssets;
import com.qozix.tileview.widgets.ScalingLayout;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TileCanvasViewGroup extends ScalingLayout implements TileCanvasView.TileCanvasDrawListener {

  private static final int RENDER_FLAG = 1;

  public static final int DEFAULT_RENDER_BUFFER = 250;
  public static final int FAST_RENDER_BUFFER = 10;
  private static final int DEFAULT_TRANSITION_DURATION = 200;

  private BitmapProvider mBitmapProvider;
  private HashMap<Float, TileCanvasView> mTileCanvasViewHashMap = new HashMap<>();

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

  private TileRenderPoolExecutor mTileRenderPoolExecutor;


  public Set<Tile> tilesInCurrentViewport = new HashSet<>();
  public Set<Tile> tilesNotInCurrentViewport = new HashSet<>();
  public Set<Tile> tilesAlreadyRendered = new HashSet<>();

  public TileCanvasViewGroup( Context context ) {
    super(context);
    setWillNotDraw( false );
    mTileRenderHandler = new TileRenderHandler( this );
    mTileRenderPoolExecutor = new TileRenderPoolExecutor();
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
    if( mTileRenderPoolExecutor != null ){
      mTileRenderPoolExecutor.cancel();
    }
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
    tilesInCurrentViewport.clear();
    mCurrentTileCanvasView.clearTiles( mShouldRecycleBitmaps );
  }

  /**
   * Effectively adds any new tiles, without replacing existing tiles, and removes those not in passed set.
   * @param recentlyComputedVisibleTileSet Tile Set that should be visible, based on DetailLevel inspection of viewport size and position.
   */
  public void reconcile( Set<Tile> recentlyComputedVisibleTileSet ){
    for( Tile tile : tilesInCurrentViewport ) {
      if( !recentlyComputedVisibleTileSet.contains( tile ) ) {
        tilesNotInCurrentViewport.add( tile );
      }
    }
    tilesInCurrentViewport.addAll( recentlyComputedVisibleTileSet );
    tilesInCurrentViewport.removeAll( tilesNotInCurrentViewport );
    tilesNotInCurrentViewport.clear();
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
    Set<Tile> visibleTiles = mDetailLevelToRender.getVisibleTilesFromLastViewportComputation();
    reconcile( visibleTiles );
    if( mTileRenderPoolExecutor != null ){
      mTileRenderPoolExecutor.queue( this, getRenderSet() );
    }
  }

  private void clearOutOfViewportTiles(){
    Set<Tile> condemned = new HashSet<>( tilesAlreadyRendered );
    condemned.removeAll( tilesInCurrentViewport );
    tilesAlreadyRendered.removeAll( condemned );
    for( Tile tile : condemned ) {
      tile.destroy( mShouldRecycleBitmaps );
    }
    mCurrentTileCanvasView.invalidate();
  }

  private void cleanup() {
    clearOutOfViewportTiles();
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
    mTileRenderHandler.post( mRenderPostExecuteRunnable );
  }

  Set<Tile> getRenderSet() {
    Set<Tile> renderSet = new HashSet<>( tilesInCurrentViewport );
    renderSet.removeAll( tilesAlreadyRendered );
    return renderSet;
  }

  void generateTileBitmap( Tile tile ) {
    tile.generateBitmap( getContext(), getBitmapProvider() );
  }

  void addTileToCurrentTileCanvasView( final Tile tile ) {
    if( !tilesInCurrentViewport.contains( tile ) ) {
      return;
    }
    tile.setTransitionsEnabled( mTransitionsEnabled );
    tile.setTransitionDuration( mTransitionDuration );
    tile.stampTime();
    tilesAlreadyRendered.add( tile );
    mCurrentTileCanvasView.addTile( tile );
  }

  void handleTileRenderException( Throwable throwable ) {
    if( throwable instanceof OutOfMemoryError ){
      cleanup();
    }
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
    mTileRenderPoolExecutor.shutdownNow();
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

    private final WeakReference<TileCanvasViewGroup> mTileCanvasViewGroupWeakReference;

    public TileRenderHandler( TileCanvasViewGroup tileCanvasViewGroup ) {
      super( Looper.getMainLooper() );
      mTileCanvasViewGroupWeakReference = new WeakReference<>( tileCanvasViewGroup );
    }

    @Override
    public final void handleMessage( Message message ) {
      final TileCanvasViewGroup tileCanvasViewGroup = mTileCanvasViewGroupWeakReference.get();
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

  // This runnable is required to run on UI thread
  private Runnable mRenderPostExecuteRunnable =  new Runnable() {
    @Override
    public void run() {
      if ( !mTransitionsEnabled ) {
        cleanup();
      }
      if( mTileRenderListener != null ) {
        mTileRenderListener.onRenderComplete();
      }
      invalidate();
      requestRender();
    }
  };
}
