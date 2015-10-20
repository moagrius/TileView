package com.qozix.tileview.tiles;

import android.content.Context;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.qozix.tileview.detail.DetailLevel;
import com.qozix.tileview.graphics.BitmapProvider;
import com.qozix.tileview.graphics.BitmapProviderAssets;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;

public class TileCanvasViewGroup extends ViewGroup implements TileCanvasView.TileCanvasDrawListener {

  private static final int RENDER_FLAG = 1;
  private static final int RENDER_BUFFER = 250;

  private static final int TRANSITION_DURATION = 200;

  private LinkedList<Tile> mScheduledToRender = new LinkedList<Tile>();
  private LinkedList<Tile> mAlreadyRendered = new LinkedList<Tile>();

  private BitmapProvider mBitmapProvider = new BitmapProviderAssets();
  private HashMap<Float, TileCanvasView> mTileCanvasViewHashMap = new HashMap<Float, TileCanvasView>();

  private TileRenderTask mLastRunTileRenderTask;

  private DetailLevel mDetailLevelToRender;
  private DetailLevel mLastRenderedDetailLevel;
  private TileCanvasView mCurrentTileCanvasView;

  private boolean mRenderIsCancelled = false;
  private boolean mRenderIsSuppressed = false;
  private boolean mIsRendering = false;

  private boolean mTransitionsEnabled = true;

  private int mTransitionDuration = TRANSITION_DURATION;

  private TileRenderHandler mTileRenderHandler;
  private TileRenderListener mTileRenderListener;

  private float mScale = 1;

  public TileCanvasViewGroup( Context context ) {
    super( context );
    setWillNotDraw( false );
    mTileRenderHandler = new TileRenderHandler( this );
  }

  public float getScale() {
    return mScale;
  }

  public void setScale( float scale ) {
    mScale = scale;
    invalidate();
  }

  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {
    int availableWidth = r - l;
    int availableHeight = b - t;
    for( int i = 0; i < getChildCount(); i++ ) {
      View child = getChildAt( i );
      if( child.getVisibility() != GONE ) {
        child.layout( 0, 0, availableWidth, availableHeight );
      }
    }
  }

  public boolean getTransitionsEnabled(){
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

  public void setBitmapProvider( BitmapProvider bitmapProvider ) {
    mBitmapProvider = bitmapProvider;
  }

  public void setTileRenderListener( TileRenderListener tileRenderListener ) {
    mTileRenderListener = tileRenderListener;
  }

  public void requestRender() {
    mRenderIsCancelled = false;
    mRenderIsSuppressed = false;
    if( mDetailLevelToRender == null ) {
      return;
    }
    if( !mTileRenderHandler.hasMessages( RENDER_FLAG ) ) {
      mTileRenderHandler.sendEmptyMessageDelayed( RENDER_FLAG, RENDER_BUFFER );
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
    mCurrentTileCanvasView.setVisibility( View.VISIBLE );
  }

  public boolean getIsRendering() {
    return mIsRendering;
  }

  public void clear() {
    // suppress and cancel renders
    suppressRender();
    cancelRender();
    // destroy all tiles
    for( Tile m : mScheduledToRender ) {
      m.destroy();
    }
    mScheduledToRender.clear();
    for( Tile m : mAlreadyRendered ) {
      m.destroy();
    }
    mAlreadyRendered.clear();
  }

  private float getCurrentDetailLevelScale(){
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

  // access omitted deliberately - need package level access for the TileRenderHandler
  void renderTiles() {
    Log.d( "Tiles", "TileManager.renderTiles" );
    // has it been canceled since it was requested?
    if( mRenderIsCancelled ) {
      return;
    }
    // can we keep rending existing tasks, but not start new ones?
    if( mRenderIsSuppressed ) {  // TODO: makes this naming scheme clearer
      return;
    }
    // fast-fail if there's no available data
    if( mDetailLevelToRender == null ) {
      return;
    }
    // getBitmap and render the bitmaps asynchronously
    beginRenderTask();
  }

  private void beginRenderTask() {
    boolean changed = mDetailLevelToRender.computeCurrentState();
    if( !changed ){
      return;
    }
    mScheduledToRender = mDetailLevelToRender.getVisibleTilesFromLastViewportComputation();
    if( mLastRunTileRenderTask != null && mLastRunTileRenderTask.getStatus() != AsyncTask.Status.FINISHED ) {
      mLastRunTileRenderTask.cancel( true );
    }
    mLastRunTileRenderTask = new TileRenderTask( this );
    mLastRunTileRenderTask.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR );
  }

  private void cleanup() {
    // start with all rendered tiles...
    LinkedList<Tile> condemned = new LinkedList<Tile>( mAlreadyRendered );
    // now remove all those that were just qualified
    condemned.removeAll( mScheduledToRender );
    // for whatever's left, destroy
    Log.d( "Tiles", "about to destroy " + condemned.size() + " tiles." );
    for( Tile m : condemned ) {
      m.destroy();
    }
    mCurrentTileCanvasView.invalidate();
    //  and remove from list of rendered tiles
    mAlreadyRendered.removeAll( condemned );
    // hide all other groups
    for( TileCanvasView tileGroup : mTileCanvasViewHashMap.values() ) {
      if( mCurrentTileCanvasView != tileGroup ) {
        tileGroup.clearTiles();
        tileGroup.setVisibility( View.GONE );
      }

    }
    invalidate();
  }

	/*
   *  render tasks (invoked in asynctask's thread)
	 */

  void onRenderTaskPreExecute() {
    // set a flag that we're working
    mIsRendering = true;
    // notify anybody interested
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
    // set flag that we're done
    mIsRendering = false;
    // everything's been rendered, so if we don't have to wait for transitions, clean up now
    if( !mTransitionsEnabled ) {
      cleanup();
    }
    // recurse - request another round of render - if the same intersections are discovered, recursion will end anyways
    requestRender();
    // notify anybody interested
    if( mTileRenderListener != null ) {
      mTileRenderListener.onRenderComplete();
    }
  }

  LinkedList<Tile> getRenderList() {
    return (LinkedList<Tile>) mScheduledToRender.clone();
  }

  // package level access so it can be invoked by the render task
  void decodeIndividualTile( Tile m ) {
    m.generateBitmap( getContext(), mBitmapProvider );
  }

  // package level access so it can be invoked by the render task
  void renderIndividualTile( Tile tile ) {
    // if it's already rendered, quit now
    if( mAlreadyRendered.contains( tile ) ) {
      return;
    }
    // do we animate?
    tile.setTransitionsEnabled( mTransitionsEnabled );
    // set duration in either case, they may be enabled later
    tile.setTransitionDuration( mTransitionDuration );
    // stamp no matter what, transitions might be enabled later
    tile.stampTime();
    // add it to the list of those rendered
    mAlreadyRendered.add( tile );
    // add it to the appropriate set (which is already scaled)
    mCurrentTileCanvasView.addTile( tile );
  }

  boolean getRenderIsCancelled() {
    return mRenderIsCancelled;
  }

  @Override
  public void onCleanDrawComplete( TileCanvasView tileCanvasView ) {
    if( mTransitionsEnabled && tileCanvasView == mCurrentTileCanvasView ) {
      cleanup();
    }
  }

  @Override
  public void onDraw( Canvas canvas ) {
    canvas.scale( mScale, mScale );
    super.onDraw( canvas );
  }

  public static class TileRenderHandler extends Handler {

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


  public interface TileRenderListener {
    void onRenderStart();
    void onRenderCancelled();
    void onRenderComplete();
  }

}
