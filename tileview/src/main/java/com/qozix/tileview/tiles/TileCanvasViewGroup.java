package com.qozix.tileview.tiles;

import android.content.Context;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.qozix.tileview.TileView;
import com.qozix.tileview.detail.DetailLevel;
import com.qozix.tileview.graphics.BitmapDecoder;
import com.qozix.tileview.graphics.BitmapDecoderAssets;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;

public class TileCanvasViewGroup extends ViewGroup implements TileCanvasView.TileCanvasDrawListener {

  private static final int RENDER_FLAG = 1;
  private static final int RENDER_BUFFER = 250;

  private static final int TRANSITION_DURATION = 200;

  private LinkedList<Tile> scheduledToRender = new LinkedList<Tile>();
  private LinkedList<Tile> alreadyRendered = new LinkedList<Tile>();

  private BitmapDecoder decoder = new BitmapDecoderAssets();
  private HashMap<Float, TileCanvasView> tileGroups = new HashMap<Float, TileCanvasView>();

  private TileRenderTask lastRunRenderTask;

  private DetailLevel detailLevelToRender;
  private DetailLevel lastRenderedDetailLevel;
  private TileCanvasView currentTileGroup;

  private boolean renderIsCancelled = false;
  private boolean renderIsSuppressed = false;
  private boolean isRendering = false;

  private boolean transitionsEnabled = true;
  private int transitionDuration = TRANSITION_DURATION;

  private TileRenderHandler handler;
  private TileRenderListener renderListener;

  private float mScale = 1;
  private int mBaseWidth;
  private int mBaseHeight;



  public TileCanvasViewGroup( Context context ) {
    super( context );
    setWillNotDraw( false );
    handler = new TileRenderHandler( this );
  }

  public float getScale() {
    return mScale;
  }

  public void setScale( float scale ) {
    mScale = scale;
    Log.d( "Tiles", "TCVG.setScale=" + scale + ", gw=" + getWidth() );
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
        child.layout( 0, 0, mBaseWidth, mBaseHeight );
      }
    }
  }

  public void setTransitionsEnabled( boolean enabled ) {
    transitionsEnabled = enabled;
  }

  public void setTransitionDuration( int duration ) {
    transitionDuration = duration;
  }

  public void setDecoder( BitmapDecoder d ) {
    decoder = d;
  }

  public void setTileRenderListener( TileRenderListener listener ) {
    renderListener = listener;
  }

  public void requestRender() {
    // if we're requesting it, we must really want one
    renderIsCancelled = false;
    renderIsSuppressed = false;
    // if there's no data about the current detail level, don't bother
    if( detailLevelToRender == null ) {
      return;
    }
    // throttle requests
    if( !handler.hasMessages( RENDER_FLAG ) ) {
      // give it enough buffer that (generally) successive calls will be captured
      handler.sendEmptyMessageDelayed( RENDER_FLAG, RENDER_BUFFER );
    }
  }

  public void cancelRender() {
    // hard cancel - further render tasks won't start, and we'll attempt to interrupt the currently executing task
    renderIsCancelled = true;
    // if the currently executing task isn't null...
    if( lastRunRenderTask != null ) {
      // ... and it's in a cancellable state
      if( lastRunRenderTask.getStatus() != AsyncTask.Status.FINISHED ) {
        // ... then squash it
        lastRunRenderTask.cancel( true );
      }
    }
    // give it to gc
    lastRunRenderTask = null;
  }

  public void suppressRender() {
    // this will prevent new tasks from starting, but won't actually cancel the currently executing task
    renderIsSuppressed = true;
  }

  public void updateTileSet( DetailLevel detailLevel ) {
    // grab reference to this detail level, so we can get it's tile set for comparison to viewport
    detailLevelToRender = detailLevel;
    // fast-fail if it's null
    if( detailLevelToRender == null ) {
      return;
    }
    // fast-fail if there's no change (same tile set)
    if( detailLevelToRender.equals( lastRenderedDetailLevel ) ) {
      return;
    }
    // we made it this far, cache the new level to test for changes on next invocation
    lastRenderedDetailLevel = detailLevelToRender;
    // fetch appropriate child
    currentTileGroup = getCurrentTileGroup();
    // show it
    currentTileGroup.setVisibility( View.VISIBLE );
    // bring it to top of stack
    currentTileGroup.bringToFront();
  }

  public boolean getIsRendering() {
    return isRendering;
  }

  public void clear() {
    // suppress and cancel renders
    suppressRender();
    cancelRender();
    // destroy all tiles
    for( Tile m : scheduledToRender ) {
      m.destroy();
    }
    scheduledToRender.clear();
    for( Tile m : alreadyRendered ) {
      m.destroy();
    }
    alreadyRendered.clear();
  }

  private float getCurrentDetailLevelScale(){
    if( detailLevelToRender != null ) {
      return detailLevelToRender.getScale();
    }
    return 1;
  }

  private TileCanvasView getCurrentTileGroup() {
    // get the registered mScale for the active detail level
    float levelScale = getCurrentDetailLevelScale();
    // if a tile group has already been created and registered...
    if( tileGroups.containsKey( levelScale ) ) {
      // ... we're done.  return cached level.
      return tileGroups.get( levelScale );
    }
    // otherwise create one
    TileCanvasView tileGroup = new TileCanvasView( getContext() );
    // listener for clean draws
    tileGroup.setTileCanvasDrawListener( this );
    // mScale it to the inverse of the levels mScale (so 0.25 levels are shown at 400%)
    // TODO: use this for inSampleSize in decoder
    tileGroup.setScale( 1 / levelScale );
    // register it mScale (key) for re-use
    tileGroups.put( levelScale, tileGroup );
    // MATCH_PARENT should work here but doesn't, roll back if reverting to FrameLayout
    // TODO: all children should match parent
    // TODO: debug
    TileView tileView = (TileView) getParent();
    Log.d( "Tiles", "tv.gbw=" + tileView.getBaseWidth() + ", gmw=" + getMeasuredWidth() + ", gw=" + getWidth() + ", lp.width=" + getLayoutParams().width );
    //addView( tileGroup, new FixedLayout.LayoutParams( tileView.getBaseWidth(), tileView.getBaseHeight() ) );
    addView( tileGroup );
    // send it off
    return tileGroup;
  }

  // access omitted deliberately - need package level access for the TileRenderHandler
  void renderTiles() {
    Log.d( "Tiles", "TileManager.renderTiles" );
    // has it been canceled since it was requested?
    if( renderIsCancelled ) {
      return;
    }
    // can we keep rending existing tasks, but not start new ones?
    if( renderIsSuppressed ) {
      return;
    }
    // fast-fail if there's no available data
    if( detailLevelToRender == null ) {
      return;
    }
    // decode and render the bitmaps asynchronously
    beginRenderTask();
  }

  private void beginRenderTask() {
    Log.d( "Tiles", "TileManager.beginRenderTask" );
    // find all matching tiles
    LinkedList<Tile> intersections = detailLevelToRender.calculateIntersections();
    // if it's the same list, don't bother
    if( scheduledToRender.equals( intersections ) ) {
      return;
    }
    // if we made it here, then replace the old list with the new list
    scheduledToRender = intersections;
    // cancel task if it's already running
    if( lastRunRenderTask != null ) {
      if( lastRunRenderTask.getStatus() != AsyncTask.Status.FINISHED ) {
        lastRunRenderTask.cancel( true );
      }
    }
    // start a new one
    lastRunRenderTask = new TileRenderTask( this );
    lastRunRenderTask.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR );
  }

  private void cleanup() {
    // start with all rendered tiles...
    LinkedList<Tile> condemned = new LinkedList<Tile>( alreadyRendered );
    // now remove all those that were just qualified
    condemned.removeAll( scheduledToRender );
    // for whatever's left, destroy
    Log.d( "Tiles", "about to destroy " + condemned.size() + " tiles." );
    for( Tile m : condemned ) {
      m.destroy();
    }
    currentTileGroup.invalidate();
    //  and remove from list of rendered tiles
    alreadyRendered.removeAll( condemned );
    // hide all other groups
    for( TileCanvasView tileGroup : tileGroups.values() ) {
      if( currentTileGroup != tileGroup ) {
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
    isRendering = true;
    // notify anybody interested
    if( renderListener != null ) {
      renderListener.onRenderStart();
    }
  }

  void onRenderTaskCancelled() {
    if( renderListener != null ) {
      renderListener.onRenderCancelled();
    }
    isRendering = false;
  }

  void onRenderTaskPostExecute() {
    // set flag that we're done
    isRendering = false;
    // everything's been rendered, so if we don't have to wait for transitions, clean up now
    if( !transitionsEnabled ) {
      cleanup();
    }
    // recurse - request another round of render - if the same intersections are discovered, recursion will end anyways
    requestRender();
    // notify anybody interested
    if( renderListener != null ) {
      renderListener.onRenderComplete();
    }
  }

  LinkedList<Tile> getRenderList() {
    return (LinkedList<Tile>) scheduledToRender.clone();
  }

  // package level access so it can be invoked by the render task
  void decodeIndividualTile( Tile m ) {
    m.decode( getContext(), decoder );
  }

  // package level access so it can be invoked by the render task
  void renderIndividualTile( Tile tile ) {
    // if it's already rendered, quit now
    if( alreadyRendered.contains( tile ) ) {
      return;
    }
    // do we animate?
    tile.setTransitionsEnabled( transitionsEnabled );
    // stamp no matter what, transitions might be enabled later
    tile.stampTime();
    // add it to the list of those rendered
    alreadyRendered.add( tile );
    // add it to the appropriate set (which is already scaled)
    currentTileGroup.addTile( tile );
  }

  boolean getRenderIsCancelled() {
    return renderIsCancelled;
  }

  @Override
  public void onCleanDrawComplete( TileCanvasView tileCanvasView ) {
    if( transitionsEnabled && tileCanvasView == currentTileGroup ) {
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
