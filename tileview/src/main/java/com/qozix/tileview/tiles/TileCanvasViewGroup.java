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

  private int mBaseWidth;
  private int mBaseHeight;

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

  public void setSize( int width, int height ) {
    mBaseWidth = width;
    mBaseHeight = height;
  }

  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {
    for( int i = 0; i < getChildCount(); i++ ) {
      View child = getChildAt( i );
      if( child.getVisibility() != GONE ) {
        child.layout( 0, 0, mBaseWidth, mBaseHeight );  // TODO: r-l,b-t
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

  public void setBitmapProvider( BitmapProvider d ) {
    mBitmapProvider = d;
  }

  public void setTileRenderListener( TileRenderListener listener ) {
    mTileRenderListener = listener;
  }

  public void requestRender() {
    // if we're requesting it, we must really want one
    mRenderIsCancelled = false;
    mRenderIsSuppressed = false;
    // if there's no data about the current detail level, don't bother
    if( mDetailLevelToRender == null ) {
      return;
    }
    // throttle requests
    if( !mTileRenderHandler.hasMessages( RENDER_FLAG ) ) {
      // give it enough buffer that (generally) successive calls will be captured
      mTileRenderHandler.sendEmptyMessageDelayed( RENDER_FLAG, RENDER_BUFFER );
    }
  }

  public void cancelRender() {
    // hard cancel - further render tasks won't start, and we'll attempt to interrupt the currently executing task
    mRenderIsCancelled = true;
    // if the currently executing task isn't null...
    if( mLastRunTileRenderTask != null ) {
      // ... and it's in a cancellable state
      if( mLastRunTileRenderTask.getStatus() != AsyncTask.Status.FINISHED ) {
        // ... then squash it
        mLastRunTileRenderTask.cancel( true );
      }
    }
    // give it to gc
    mLastRunTileRenderTask = null;
  }

  public void suppressRender() {
    // this will prevent new tasks from starting, but won't actually cancel the currently executing task
    mRenderIsSuppressed = true;
  }

  public void updateTileSet( DetailLevel detailLevel ) {
    // grab reference to this detail level, so we can get it's tile set for comparison to viewport
    mDetailLevelToRender = detailLevel;
    // fast-fail if it's null
    if( mDetailLevelToRender == null ) {
      return;
    }
    // fast-fail if there's no change (same tile set)
    if( mDetailLevelToRender.equals( mLastRenderedDetailLevel ) ) {
      return;
    }
    // we made it this far, cache the new level to test for changes on next invocation
    mLastRenderedDetailLevel = mDetailLevelToRender;
    // fetch appropriate child
    mCurrentTileCanvasView = getCurrentTileCanvasView();
    // show it
    mCurrentTileCanvasView.setVisibility( View.VISIBLE );
    // bring it to top of stack
    mCurrentTileCanvasView.bringToFront();
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
        tileGroup.setVisibility( View.GONE );
        tileGroup.clearTiles();
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
      Log.d( "Tiles", "current group is done rendering including transitions, do cleanup" );
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
