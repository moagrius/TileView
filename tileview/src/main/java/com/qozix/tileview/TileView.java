package com.qozix.tileview;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.qozix.tileview.detail.DetailLevel;
import com.qozix.tileview.detail.DetailLevelManager;
import com.qozix.tileview.geom.CoordinateTranslater;
import com.qozix.tileview.graphics.BitmapDecoder;
import com.qozix.tileview.graphics.BitmapDecoderHttp;
import com.qozix.tileview.hotspots.HotSpot;
import com.qozix.tileview.hotspots.HotSpotEventListener;
import com.qozix.tileview.hotspots.HotSpotManager;
import com.qozix.tileview.layouts.AnchorLayout;
import com.qozix.tileview.layouts.ScalingLayout;
import com.qozix.tileview.layouts.ZoomPanLayout;
import com.qozix.tileview.markers.CalloutLayout;
import com.qozix.tileview.markers.MarkerEventListener;
import com.qozix.tileview.markers.MarkerLayout;
import com.qozix.tileview.paths.CompositePathView;
import com.qozix.tileview.paths.DrawablePath;
import com.qozix.tileview.tiles.TileCanvasViewGroup;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * The TileView widget is a subclass of ViewGroup that provides a mechanism to asynchronously display tile-based images,
 * with additional functionality for 2D dragging, flinging, pinch or double-tap to zoom, adding overlaying Views (markers),
 * built-in Hot Spot support, dynamic path drawing, multiple levels of detail, and support for any relative positioning or
 * coordinate system.
 * <p/>
 * <p>A minimal implementation:</p>
 * <p/>
 * <pre>{@code
 * TileView tileView = new TileView(this);
 * tileView.setSize(3000,5000);
 * tileView.addDetailLevel(1.0f, "path/to/tiles/%col%-%row%.jpg");
 * }</pre>
 * <p/>
 * A more advanced implementation might look like:
 * <pre>{@code
 * TileView tileView = new TileView(this);
 * tileView.setSize(3000,5000);
 * tileView.addTileViewEventListener(someMapEventListener);
 * tileView.defineBounds(42.379676, -71.094919, 42.346550, -71.040280);
 * tileView.addDetailLevel(1.000f, "tiles/boston-1000-%col%_%row%.jpg", 256, 256);
 * tileView.addDetailLevel(0.500f, "tiles/boston-500-%col%_%row%.jpg", 256, 256);
 * tileView.addDetailLevel(0.250f, "tiles/boston-250-%col%_%row%.jpg", 256, 256);
 * tileView.addDetailLevel(0.125f, "tiles/boston-125-%col%_%row%.jpg", 128, 128);
 * tileView.addMarker(someView, 42.35848, -71.063736);
 * tileView.addMarker(anotherView, 42.3665, -71.05224);
 * tileView.addMarkerEventListener(someMarkerEventListener);
 * }</pre>
 */
public class TileView extends ZoomPanLayout implements
  ZoomPanLayout.ZoomPanListener,
  TileCanvasViewGroup.TileRenderListener,
  DetailLevelManager.DetailLevelChangeListener {

  private static final int DEFAULT_TILE_SIZE = 256;

  private DetailLevelManager mDetailLevelManager = new DetailLevelManager();
  private CoordinateTranslater mCoordinateTranslater = new CoordinateTranslater();
  private HotSpotManager hotSpotManager = new HotSpotManager();

  private TileCanvasViewGroup mTileCanvasViewGroup;
  private CompositePathView mCompositePathView;
  private ScalingLayout mScalingLayout;
  private MarkerLayout mMarkerLayout;
  private CalloutLayout mCalloutLayout;

  private RenderThrottleHandler mRenderThrottleHandler;

  /**
   * Constructor to use when creating a TileView from code.
   *
   * @param context (Context) The Context the TileView is running in, through which it can access the current theme, resources, etc.
   */
  public TileView( Context context ) {
    this( context, null );
  }

  public TileView( Context context, AttributeSet attrs ) {
    this( context, attrs, 0 );
  }

  public TileView( Context context, AttributeSet attrs, int defStyleAttr ) {
    super( context, attrs, defStyleAttr );

    mTileCanvasViewGroup = new TileCanvasViewGroup( context );
    addView( mTileCanvasViewGroup );

    mCompositePathView = new CompositePathView( context );
    addView( mCompositePathView );

    mScalingLayout = new ScalingLayout( context );
    addView( mScalingLayout );

    mMarkerLayout = new MarkerLayout( context );
    // TODO: listen to markers here?
    addView( mMarkerLayout );

    mCalloutLayout = new CalloutLayout( context );
    addView( mCalloutLayout );

    mDetailLevelManager.setDetailLevelChangeListener( this );
    mTileCanvasViewGroup.setTileRenderListener( this );
    addZoomPanListener( this );

    mRenderThrottleHandler = new RenderThrottleHandler( this );  // TODO: cleanup

    requestRender();

  }

  //------------------------------------------------------------------------------------
  // Layers API
  //------------------------------------------------------------------------------------

  public void addScalingView( View view, int index, int x, int y ) {
    ScalingLayout.LayoutParams layoutParams = new ScalingLayout.LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, x, y );
    mScalingLayout.addView( view, index, layoutParams );
  }


  //------------------------------------------------------------------------------------
  // Rendering API
  //------------------------------------------------------------------------------------

  /**
   * Request that the current tile set is re-examined and re-drawn.
   * The request is added to a queue and is not guaranteed to be processed at any particular
   * time, and will never be handled immediately.
   */
  public void requestRender() {
    mTileCanvasViewGroup.requestRender();
  }

  /**
   * While all render operation requests are queued and batched, this method provides an additional
   * throttle layer, so that any subsequent invocations cancel and pending invocations.
   * <p/>
   * This is useful when requesting in a stream fashion, either in a loop or in response to a
   * progressive action like an animation or touch move.
   */
  public void requestThrottledRender() {
    mRenderThrottleHandler.submit();
  }

  /**
   * If flinging, defer render, otherwise request now.
   * If a render operation starts at the beginning of a fling, a stutter can occur.
   */
  private void requestSafeRender() {
    if( isFlinging() ) {
      // TODO: does this really help?
      requestThrottledRender();
    } else {
      requestRender();
    }
  }

  /**
   * Notify the TileView that it may stop rendering tiles.  The rendering thread will be
   * sent an interrupt request, but no guarantee is provided when the request will be responded to.
   */
  public void cancelRender() {
    mTileCanvasViewGroup.cancelRender();
  }

  /**
   * Notify the TileView that it should continue to render any pending tiles, but should not
   * accept new render tasks.
   */
  public void suppressRender() {
    mTileCanvasViewGroup.suppressRender();
  }

  /**
   * Sets a custom class to perform the decode operation when tile bitmaps are requested for tile images only.
   * By default, a BitmapDecoder implementation is provided that renders bitmaps from the context's Assets,
   * but alternative implementations could be used that fetch images via HTTP, or from the SD card, or resources, SVG, etc.
   * {@link BitmapDecoderHttp} is an example of such an implementation.
   *
   * @param decoder (BitmapDecoder) A class instance that implements BitmapDecoder, and must define a decode method, which accepts a String file name and a Context object, and returns a Bitmap
   */
  public void setTileDecoder( BitmapDecoder decoder ) {
    mTileCanvasViewGroup.setDecoder( decoder );
  }

  /**
   * Defines whether tile bitmaps should be rendered using an AlphaAnimation
   *
   * @param enabled (boolean) true if the TileView should render tiles with fade transitions
   */
  public void setTransitionsEnabled( boolean enabled ) {
    mTileCanvasViewGroup.setTransitionsEnabled( enabled );
  }

  /**
   * Define the duration (in milliseconds) for each tile transition.
   *
   * @param duration (int) the duration of the transition in milliseconds.
   */
  public void setTransitionDuration( int duration ) {
    mTileCanvasViewGroup.setTransitionDuration( duration );
  }

  //------------------------------------------------------------------------------------
  // Detail Level Management API
  //------------------------------------------------------------------------------------

  /**
   * Defines the total size, in pixels, of the tile set at 100% mScale.
   * The TileView wills pan within it's layout dimensions, with the content (scrollable)
   * size defined by this method.
   *
   * @param width  (int) total width of the tiled set
   * @param height (int) total height of the tiled set
   */
  @Override
  public void setSize( int width, int height ) {
    super.setSize( width, height );
    mDetailLevelManager.setSize( width, height );
    mCoordinateTranslater.setSize( width, height );
    mTileCanvasViewGroup.setSize( width, height );
  }

  /**
   * Register a tile set to be used for a particular detail level.
   * Each tile set to be used must be registered using this method,
   * and at least one tile set must be registered for the TileView to render any tiles.
   *
   * @param detailScale Scale at which the TileView should use the tiles in this set.
   * @param data        An arbitrary object of any type that is passed to the (Adapter|Decoder) for each tile on this level.
   */
  public void addDetailLevel( float detailScale, Object data ) {
    addDetailLevel( detailScale, data, DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE );
  }

  /**
   * Register a tile set to be used for a particular detail level.
   * Each tile set to be used must be registered using this method,
   * and at least one tile set must be registered for the TileView to render any tiles.
   *
   * @param detailScale (float) mScale at which the TileView should use the tiles in this set.
   * @param data        An arbitrary object of any type that is passed to the (Adapter|Decoder) for each tile on this level.
   * @param tileWidth   (int) size of each tiled column
   * @param tileHeight  (int) size of each tiled row
   */
  public void addDetailLevel( float detailScale, Object data, int tileWidth, int tileHeight ) {
    mDetailLevelManager.addDetailLevel( detailScale, data, tileWidth, tileHeight );
  }

  /**
   * Clear all previously registered zoom levels.  This method is experimental.
   */
  public void resetDetailLevels() {
    mDetailLevelManager.resetDetailLevels();
    refresh();
  }

  /**
   * While the detail level is locked (after this method is invoked, and before unlockDetailLevel is invoked),
   * the DetailLevel will not change, and the current DetailLevel will be scaled beyond the normal
   * bounds.  Normally, during any mScale change the details manager searches for the DetailLevel with
   * a registered mScale closest to the defined mScale.  While locked, this does not occur.
   */
  public void lockDetailLevel() {
    mDetailLevelManager.lockDetailLevel();
  }

  /**
   * Unlocks a DetailLevel locked with lockDetailLevel
   */
  public void unlockDetailLevel() {
    mDetailLevelManager.unlockDetailLevel();
  }

  /**
   * pads the viewport by the number of pixels passed.  e.g., setViewportPadding( 100 ) instructs the
   * TileView to interpret it's actual viewport offset by 100 pixels in each direction (top, left,
   * right, bottom), so more tiles will qualify for "visible" status when intersections are calculated.
   *
   * @param padding (int) the number of pixels to pad the viewport by
   */
  public void setViewportPadding( int padding ) {
    mDetailLevelManager.setViewportPadding( padding );
  }

  //------------------------------------------------------------------------------------
  // Positioning API
  //------------------------------------------------------------------------------------

  /**
   * Register a set of offset points to use when calculating position within the TileView.
   * Any type of coordinate system can be used (any type of lat/lng, percentile-based, etc),
   * and all positioned are calculated relatively.  If relative bounds are defined, position parameters
   * received by TileView methods will be translated to the the appropriate pixel value.
   * To remove this process, use undefineBounds.
   *
   * @param left   the left edge of the rectangle used when calculating position.
   * @param top    the top edge of the rectangle used when calculating position.
   * @param right  the right edge of the rectangle used when calculating position.
   * @param bottom the bottom edge of the rectangle used when calculating position.
   */
  public void defineBounds( double left, double top, double right, double bottom ) {
    mCoordinateTranslater.setBounds( left, top, right, bottom );
  }

  /**
   * Unregisters arbitrary bounds and coordinate system.  After invoking this method, TileView methods that
   * receive position method parameters will use pixel values, relative to the TileView's registered size (at 1.0d mScale)
   */
  public void undefineBounds() {
    mCoordinateTranslater.unsetBounds();
  }

  /**
   * Scrolls (instantly) the TileView to the x and y positions provided.
   *
   * @param x The relative x position to move to.
   * @param y The relative y position to move to.
   */
  public void scrollTo( double x, double y ) {
    scrollTo(
      mCoordinateTranslater.translateAndScaleX( x, getScale() ),
      mCoordinateTranslater.translateAndScaleY( y, getScale() )
    );
  }

  /**
   * Scrolls (instantly) the TileView to the x and y positions provided, then centers the viewport to the position.
   *
   * @param x The relative x position to move to.
   * @param y The relative y position to move to.
   */
  public void scrollToAndCenter( double x, double y ) {
    scrollToAndCenter(
      mCoordinateTranslater.translateAndScaleX( x, getScale() ),
      mCoordinateTranslater.translateAndScaleY( y, getScale() )
    );
  }

  /**
   * Scrolls (with animation) the TileView to the relative x and y positions provided.
   *
   * @param x The relative x position to move to.
   * @param y The relative y position to move to.
   */
  public void slideTo( double x, double y ) {
    slideTo(
      mCoordinateTranslater.translateAndScaleX( x, getScale() ),
      mCoordinateTranslater.translateAndScaleY( y, getScale() )
    );
  }

  /**
   * Scrolls (with animation) the TileView to the x and y positions provided, then centers the viewport to the position.
   *
   * @param x The relative x position to move to.
   * @param y The relative y position to move to.
   */
  public void slideToAndCenter( double x, double y ) {
    slideToAndCenter(
      mCoordinateTranslater.translateAndScaleX( x, getScale() ),
      mCoordinateTranslater.translateAndScaleY( y, getScale() )
    );
  }


  //------------------------------------------------------------------------------------
  // Marker, Callout and HotSpot API
  //------------------------------------------------------------------------------------

  /**
   * Markers added to this TileView will have anchor logic applied on the values provided here.
   * E.g., setMarkerAnchorPoints(-0.5f, -1.0f) will have markers centered horizontally, positioned
   * vertically to a value equal to - 1 * height.
   * Note that individual markers can be assigned specific anchors - this method applies a default
   * value to all markers added without specifying anchor values.
   *
   * @param anchorX The x-axis position of a marker will be offset by a number equal to the width of the marker multiplied by this value
   * @param anchorY The y-axis position of a marker will be offset by a number equal to the height of the marker multiplied by this value
   */
  public void setMarkerAnchorPoints( float anchorX, float anchorY ) {
    mMarkerLayout.setAnchors( anchorX, anchorY );
  }

  /**
   * Add a marker to the the TileView.  The marker can be any View.
   * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
   *
   * @param view    View instance to be added to the TileView
   * @param x       relative x position the View instance should be positioned at
   * @param y       relative y position the View instance should be positioned at
   * @param anchorX the x-axis position of a marker will be offset by a number equal to the width of the marker multiplied by this value
   * @param anchorY the y-axis position of a marker will be offset by a number equal to the height of the marker multiplied by this value
   * @return  the View instance added to the TileView
   */
  public View addMarker( View view, double x, double y, Float anchorX, Float anchorY ) {
    return mMarkerLayout.addMarker( view,
      mCoordinateTranslater.translateX( x ),
      mCoordinateTranslater.translateY( y ),
      anchorX,
      anchorY
    );
  }

  /**
   * Removes a marker View from the TileView's view tree.
   *
   * @param view (View) The marker View to be removed.
   */
  public void removeMarker( View view ) {
    mMarkerLayout.removeMarker( view );
  }

  /**
   * Moves an existing marker to another position.
   *
   * @param view    The marker View to be repositioned.
   * @param x       relative x position the View instance should be positioned at
   * @param y       relative y position the View instance should be positioned at
   * @param anchorX the x-axis position of a marker will be offset by a number equal to the width of the marker multiplied by this value
   * @param anchorY the y-axis position of a marker will be offset by a number equal to the height of the marker multiplied by this value
   */
  public void moveMarker( View view, double x, double y, Float anchorX, Float anchorY ) {
    mMarkerLayout.moveMarker( view,
      mCoordinateTranslater.translateX( x ),
      mCoordinateTranslater.translateY( y ),
      anchorX, anchorY );
  }

  /**
   * Scroll the TileView so that the View passed is centered in the viewport
   *
   * @param view    (View) the View marker that the TileView should center on.
   * @param animate (boolean) should the movement use a transition effectg
   */
  public void moveToMarker( View view, boolean animate ) {
    if( mMarkerLayout.indexOfChild( view ) > -1 ) {
      ViewGroup.LayoutParams params = view.getLayoutParams();
      if( params instanceof AnchorLayout.LayoutParams ) {  // TODO: MarkerLayout.LayoutParams
        AnchorLayout.LayoutParams anchorLayoutParams = (AnchorLayout.LayoutParams) params;
        int scaledX = (int) (anchorLayoutParams.x * getScale() + 0.5);
        int scaledY = (int) (anchorLayoutParams.y * getScale() + 0.5);
        if( animate ) {
          slideToAndCenter( scaledX, scaledY );
        } else {
          scrollToAndCenter( scaledX, scaledY );
        }
      }
    }
  }

  /**
   * Register a MarkerEventListener.  Unlike standard touch events attached to marker View's (e.g., View.OnClickListener),
   * MarkerEventListeners do not consume the touch event, so will not interfere with scrolling.  While the event is
   * dispatched from a Tap event, it's routed though a hit detection API to trigger the listener.
   *
   * @param listener Listener to be added to the TileView's list of MarkerEventListeners
   */
  public void addMarkerEventListener( MarkerEventListener listener ) {
    mMarkerLayout.addMarkerEventListener( listener );
  }

  /**
   * Removes a MarkerEventListener from the TileView's registry.
   *
   * @param listener Listener to be removed From the TileView's list of MarkerEventListeners
   */
  public void removeMarkerEventListener( MarkerEventListener listener ) {
    mMarkerLayout.removeMarkerEventListener( listener );
  }

  /**
   * Add a callout to the the TileView.  The callout can be any View.
   * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
   * Callout views will always be positioned at the top of the view tree (at the highest z-index), and will always be removed during any touch event
   * that is not consumed by the callout View.
   *
   * @param view    View instance to be added to the TileView's
   * @param x       relative x position the View instance should be positioned at
   * @param y       relative y position the View instance should be positioned at
   * @param anchorX the x-axis position of a callout view will be offset by a number equal to the width of the callout view multiplied by this value
   * @param anchorY the y-axis position of a callout view will be offset by a number equal to the height of the callout view multiplied by this value
   * @return the View instance added to the TileView's
   */
  public View addCallout( View view, double x, double y, Float anchorX, Float anchorY ) {
    return mCalloutLayout.addMarker( view,
      mCoordinateTranslater.translateX( x ),
      mCoordinateTranslater.translateY( y ),
      anchorX,
      anchorY
    );
  }

  /**
   * Removes a callout View from the TileView's view tree.
   *
   * @param view The callout View to be removed.
   */
  public void removeCallout( View view ) {
    mCalloutLayout.removeMarker( view );
  }

  /**
   * Register a HotSpot that should fire an listener when a touch event occurs that intersects that rectangle.
   * The HotSpot moves and scales with the TileView.
   *
   * @param hotSpot The hotspot that is tested against touch events that occur on the TileView
   * @return The hotspot created with this method
   */
  public HotSpot addHotSpot( HotSpot hotSpot ) {
    hotSpotManager.addHotSpot( hotSpot );
    return hotSpot;
  }

  /**
   * Register a HotSpot that should fire an listener when a touch event occurs that intersects that rectangle.
   * The HotSpot moves and scales with the TileView.
   *
   * @param positions (List<double[]>) List of paired doubles { x, y } that represents the points that make up the region.
   * @return HotSpot the hotspot created with this method
   */
  public HotSpot addHotSpot( List<double[]> positions, HotSpotEventListener listener ) {
    Path path = mCoordinateTranslater.pathFromPositions( positions );
    RectF bounds = new RectF();
    path.computeBounds( bounds, true );
    Rect rect = new Rect();
    bounds.round( rect );
    Region clip = new Region( rect );
    HotSpot hotSpot = new HotSpot();
    hotSpot.setPath( path, clip );
    hotSpot.setHotSpotEventListener( listener );
    return addHotSpot( hotSpot );
  }

  /**
   * Remove a HotSpot registered with addHotSpot
   *
   * @param hotSpot (HotSpot) the hotspot to remove
   */
  public void removeHotSpot( HotSpot hotSpot ) {
    hotSpotManager.removeHotSpot( hotSpot );
  }

  /**
   * Register a HotSpotEventListener with the TileView.  This listener will fire if any hotspot's region intersects a Tap event.
   *
   * @param listener (HotSpotEventListener) the listener to be added.
   */
  public void addHotSpotEventListener( HotSpotEventListener listener ) {
    hotSpotManager.addHotSpotEventListener( listener );
  }

  /**
   * Remove a HotSpotEventListener from the TileView's registry.
   *
   * @param listener (HotSpotEventListener) the listener to be removed
   */
  public void removeHotSpotEventListener( HotSpotEventListener listener ) {
    hotSpotManager.removeHotSpotEventListener( listener );
  }

  //------------------------------------------------------------------------------------
  // Path Drawing API
  //------------------------------------------------------------------------------------

  /**
   * Register a Path and Paint that will be drawn on a layer above the tiles, but below markers.
   * This Path's will be scaled with the TileView, but will always be as wide as the stroke set for the Paint.
   *
   * @param drawablePath (DrawablePath) a DrawablePath instance to be drawn by the TileView
   * @return DrawablePath the DrawablePath instance passed to the TileView
   */
  public DrawablePath drawPath( DrawablePath drawablePath ) {
    return mCompositePathView.addPath( drawablePath );
  }

  /**
   * Register a Path and Paint that will be drawn on a layer above the tiles, but below markers.
   * This Path's will be scaled with the TileView, but will always be as wide as the stroke set for the Paint.
   *
   * @param positions List of doubles { x, y } that represent the points of the Path.
   * @param paint     the Paint instance that defines the style of the drawn path.
   * @return the DrawablePath instance passed to the TileView
   */
  public DrawablePath drawPath( List<double[]> positions, Paint paint ) {
    Path path = mCoordinateTranslater.pathFromPositions( positions );
    return mCompositePathView.addPath( path, paint );
  }

  /**
   * Removes a DrawablePath from the TileView's registry.  This path will no longer be drawn by the TileView.
   *
   * @param drawablePath (DrawablePath) the DrawablePath instance to be removed.
   */
  public void removePath( DrawablePath drawablePath ) {
    mCompositePathView.removePath( drawablePath );
  }

  /**
   * Returns the Paint instance used by default.  This can be modified for future Path paint operations.
   *
   * @return Paint the Paint instance used by default.
   */
  public Paint getPathPaint() {
    return mCompositePathView.getPaint();
  }

  //------------------------------------------------------------------------------------
  // Memory Management API
  //------------------------------------------------------------------------------------

  /**
   * Clear bitmap image files, appropriate for Activity.onPause
   */
  public void clear() {
    mTileCanvasViewGroup.clear();
    mCompositePathView.setShouldDraw( false );
  }

  /**
   * Clear bitmap image files, appropriate for Activity.onPause (mirror for .clear)
   */
  public void pause() {
    clear();
  }

  /**
   * Clear tile image files and remove all views, appropriate for Activity.onDestroy
   * References to TileView should be set to null following invocations of this method.
   */
  public void destroy() {
    mTileCanvasViewGroup.clear();
    mCompositePathView.clear();
  }

  /**
   * Restore visible state (generally after a call to .clear()
   * Appropriate for Activity.onResume
   */
  public void resume() {
    updateViewport();
    mTileCanvasViewGroup.requestRender();
    mCompositePathView.setShouldDraw( true );
  }

  /**
   * Request the TileView reevaluate tile sets, rendered tiles, samples, invalidates, etc
   */
  public void refresh() {
    updateViewport();
    mTileCanvasViewGroup.updateTileSet( mDetailLevelManager.getCurrentDetailLevel() );
    mTileCanvasViewGroup.requestRender();
    redraw();
  }

  //------------------------------------------------------------------------------------
  // PRIVATE API
  //------------------------------------------------------------------------------------


  // make sure we keep the viewport UTD, and if layout changes we'll need to recompute what tiles to show
  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {
    super.onLayout( changed, l, t, r, b );
    updateViewport();
    requestRender();
  }

  // let the zoom manager know what tiles to show based on our position and dimensions
  private void updateViewport() {
    int left = getScrollX();
    int top = getScrollY();
    int right = left + getWidth();
    int bottom = top + getHeight();
    Log.d( "Tiles", "tv.uvp=" + getWidth() + " (should be screen size, not map size" );
    mDetailLevelManager.updateViewport( left, top, right, bottom );
  }


  //------------------------------------------------------------------------------------
  // start hooks
  //------------------------------------------------------------------------------------

  // start View
  @Override
  protected void onScrollChanged( int l, int t, int oldl, int oldt ) {
    super.onScrollChanged( l, t, oldl, oldt );
    updateViewport();
    requestThrottledRender();
  }
  // end View

  // start ZoomPanLayout
  @Override
  public void onScaleChanged( float scale, float previous ) {
    super.onScaleChanged( scale, previous );
    mDetailLevelManager.setScale( scale );
    hotSpotManager.setScale( scale );
    mTileCanvasViewGroup.setScale( scale );
    mScalingLayout.setScale( scale );
    mCompositePathView.setScale( scale );
    mMarkerLayout.setScale( scale );
    mCalloutLayout.setScale( scale );
  }
  // end ZoomPanLayout

  // start ZoomPanListener
  @Override
  public void onPanBegin( int x, int y, Origination origin ) {
    suppressRender();
  }

  @Override
  public void onPanUpdate( int x, int y, Origination origin ) {

  }

  @Override
  public void onPanEnd( int x, int y, Origination origin ) {
    requestSafeRender();
  }

  @Override
  public void onZoomBegin( float scale, float focusX, float focusY, Origination origin ) {
    mDetailLevelManager.lockDetailLevel();
    mDetailLevelManager.setScale( scale );
  }

  @Override
  public void onZoomUpdate( float scale, float focusX, float focusY, Origination origin ) {

  }

  @Override
  public void onZoomEnd( float scale, float focusX, float focusY, Origination origin ) {
    mDetailLevelManager.unlockDetailLevel();
    mDetailLevelManager.setScale( scale );
    requestRender();
  }
  // end ZoomPanListener

  // start DetailLevelChangeListener
  @Override
  public void onDetailLevelChanged( DetailLevel detailLevel ) {
    requestRender();
    mTileCanvasViewGroup.updateTileSet( detailLevel );
  }
  // end DetailLevelChangeListener

  // start OnDoubleTapListener
  @Override
  public boolean onSingleTapConfirmed( MotionEvent event ) {
    // TODO: test
    int x = (int) (getScrollX() + event.getX());
    int y = (int) (getScrollY() + event.getY());
    Point point = new Point( x, y );
    mMarkerLayout.processHit( point );
    hotSpotManager.processHit( point );
    return super.onSingleTapConfirmed( event );
  }
  // end OnDoubleTapListener

  // start TileRenderListener
  @Override
  public void onRenderStart() {

  }

  @Override
  public void onRenderCancelled() {

  }

  @Override
  public void onRenderComplete() {

  }
  // end TileRenderListener

  //------------------------------------------------------------------------------------
  // end hooks
  //------------------------------------------------------------------------------------

  //------------------------------------------------------------------------------------
  // start high-level accessors
  //------------------------------------------------------------------------------------

  public DetailLevelManager getDetailLevelManager() {
    return mDetailLevelManager;
  }

  public CoordinateTranslater getCoordinateTranslater() {
    return mCoordinateTranslater;
  }

  public HotSpotManager getHotSpotManager() {
    return hotSpotManager;
  }

  public CompositePathView getCompositePathView() {
    return mCompositePathView;
  }

  public TileCanvasViewGroup getTileCanvasViewGroup() {
    return mTileCanvasViewGroup;
  }

  public MarkerLayout getMarkerLayout() {
    return mMarkerLayout;
  }

  public CalloutLayout getCalloutLayout() {
    return mCalloutLayout;
  }

  public ScalingLayout getScalingLayout() {
    return mScalingLayout;
  }

  //------------------------------------------------------------------------------------
  // end high-level accessors
  //------------------------------------------------------------------------------------

  //------------------------------------------------------------------------------------
  // private internal listeners
  //------------------------------------------------------------------------------------


  //------------------------------------------------------------------------------------
  // end internal listeners
  //------------------------------------------------------------------------------------

  //------------------------------------------------------------------------------------
  // private internal classes
  //------------------------------------------------------------------------------------
  private static class RenderThrottleHandler extends Handler {
    private static final int MESSAGE = 0;
    private static final int RENDER_THROTTLE_TIMEOUT = 100;
    private final WeakReference<TileView> mTileViewWeakReference;

    public RenderThrottleHandler( TileView tileView ) {
      super();
      mTileViewWeakReference = new WeakReference<TileView>( tileView );
    }

    @Override
    public void handleMessage( Message msg ) {
      TileView tileView = mTileViewWeakReference.get();
      if( tileView != null ) {
        tileView.requestSafeRender();
      }
    }

    public void clear() {
      if( hasMessages( MESSAGE ) ) {
        removeMessages( MESSAGE );
      }
    }

    public void submit() {
      clear();
      sendEmptyMessageDelayed( MESSAGE, RENDER_THROTTLE_TIMEOUT );
    }
  }
  //------------------------------------------------------------------------------------
  // end internal classes
  //------------------------------------------------------------------------------------


}