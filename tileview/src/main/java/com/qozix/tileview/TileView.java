package com.qozix.tileview;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.qozix.tileview.detail.DetailLevel;
import com.qozix.tileview.detail.DetailLevelManager;
import com.qozix.tileview.geom.CoordinateTranslater;
import com.qozix.tileview.geom.FloatMathHelper;
import com.qozix.tileview.graphics.BitmapProvider;
import com.qozix.tileview.hotspots.HotSpot;
import com.qozix.tileview.hotspots.HotSpotManager;
import com.qozix.tileview.markers.CalloutLayout;
import com.qozix.tileview.markers.MarkerLayout;
import com.qozix.tileview.paths.CompositePathView;
import com.qozix.tileview.tiles.TileCanvasViewGroup;
import com.qozix.tileview.widgets.ScalingLayout;
import com.qozix.tileview.widgets.ZoomPanLayout;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * The TileView widget is a subclass of ViewGroup that supports:
 * 1. Memory-managed tiled images with multiple levels of detail.
 * 2. Panning by drag and fling.
 * 3. Zooming by pinch and double-tap.
 * 4. Markers and info windows.
 * 5. Arbitrary coordinate systems.
 * 6. Tappable hot spots.
 * 7. Path drawing.
 *
 * A minimal implementation might look like this:
 *
 * <pre>{@code
 * TileView tileView = new TileView( this );
 * tileView.setSize( 3000, 5000 );
 * tileView.addDetailLevel( 1.0f, "path/to/tiles/%d-%d.jpg" );
 * }</pre>
 *
 * A more advanced implementation might look like:
 *
 * <pre>{@code
 * TileView tileView = new TileView( this );
 * tileView.setSize( 3000, 5000 );
 * tileView.defineBounds( 42.379676, -71.094919, 42.346550, -71.040280 );
 * tileView.addDetailLevel( 1.000f, "path/to/tiles/1000/%d-%d.jpg", 256, 256 );
 * tileView.addDetailLevel( 0.500f, "path/to/tiles/500/%d-%d.jpg", 256, 256 );
 * tileView.addDetailLevel( 0.250f, "path/to/tiles/250/%d-%d.jpg", 256, 256 );
 * tileView.addDetailLevel( 0.125f, "path/to/tiles/125/%d-%d.jpg", 128, 128 );
 * tileView.addMarker( someView, 42.35848, -71.063736, null, null );
 * tileView.addMarker( anotherView, 42.3665, -71.05224, -1.0f, -0.5f );
 * tileView.setMarkerTapListener( someMarkerTapListenerImplementation );
 * }</pre>
 */
public class TileView extends ZoomPanLayout implements
  ZoomPanLayout.ZoomPanListener,
  TileCanvasViewGroup.TileRenderListener,
  DetailLevelManager.DetailLevelChangeListener {

  protected static final int DEFAULT_TILE_SIZE = 256;

  private DetailLevelManager mDetailLevelManager = new DetailLevelManager();
  private CoordinateTranslater mCoordinateTranslater = new CoordinateTranslater();
  private HotSpotManager mHotSpotManager = new HotSpotManager();

  private TileCanvasViewGroup mTileCanvasViewGroup;
  private CompositePathView mCompositePathView;
  private ScalingLayout mScalingLayout;
  private MarkerLayout mMarkerLayout;
  private CalloutLayout mCalloutLayout;

  private RenderThrottleHandler mRenderThrottleHandler;

  private boolean mShouldRenderWhilePanning = false;
  private boolean mShouldUpdateDetailLevelWhileZooming = false;

  /**
   * Constructor to use when creating a TileView from code.
   *
   * @param context The Context the TileView is running in, through which it can access the current theme, resources, etc.
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
    addView( mMarkerLayout );

    mCalloutLayout = new CalloutLayout( context );
    addView( mCalloutLayout );

    mDetailLevelManager.setDetailLevelChangeListener( this );
    mTileCanvasViewGroup.setTileRenderListener( this );
    addZoomPanListener( this );

    mRenderThrottleHandler = new RenderThrottleHandler( this );

    requestRender();

  }

  /**
   * Returns the DetailLevelManager instance used by the TileView to coordinate DetailLevels.
   *
   * @return The DetailLevelManager instance.
   */
  public DetailLevelManager getDetailLevelManager() {
    return mDetailLevelManager;
  }

  /**
   * Returns the CoordinateTranslater instance used by the TileView to manage abritrary coordinate
   * systems.
   *
   * @return The CoordinateTranslater instance.
   */
  public CoordinateTranslater getCoordinateTranslater() {
    return mCoordinateTranslater;
  }

  /**
   * Returns the HotSpotManager instance used by the TileView to detect and react to touch events
   * that intersect a user-defined region.
   *
   * @return The HotSpotManager instance.
   */
  public HotSpotManager getHotSpotManager() {
    return mHotSpotManager;
  }

  /**
   * Returns the CompositePathView instance used by the TileView to draw and scale paths.
   *
   * @return The CompositePathView instance.
   */
  public CompositePathView getCompositePathView() {
    return mCompositePathView;
  }

  /**
   * Returns the TileCanvasViewGroup instance used by the TileView to manage tile bitmap rendering.
   *
   * @return The TileCanvasViewGroup instance.
   */
  public TileCanvasViewGroup getTileCanvasViewGroup() {
    return mTileCanvasViewGroup;
  }

  /**
   * Returns the MakerLayout instance used by the TileView to position and display Views used
   * as markers.
   *
   * @return The MarkerLayout instance.
   */
  public MarkerLayout getMarkerLayout() {
    return mMarkerLayout;
  }

  /**
   * Returns the CalloutLayout instance used by the TileView to position and display Views used
   * as callouts.
   *
   * @return The CalloutLayout instance.
   */
  public CalloutLayout getCalloutLayout() {
    return mCalloutLayout;
  }

  /**
   * Returns the ScalingLayout instance used by the TileView to allow insertion of arbitrary
   * Views and ViewGroups that will scale visually with the TileView.
   *
   * @return The ScalingLayout instance.
   */
  public ScalingLayout getScalingLayout() {
    return mScalingLayout;
  }

  /**
   * Add a ViewGroup to the TileView at a z-index above tiles and paths but beneath
   * markers and callouts.  The ViewGroup will be laid out to the full dimensions of the largest
   * detail level, and will scale with the TileView.
   * Note that only the drawing surface of the view is scaled, other operations that depend
   * on dimensions are not (e.g., hit areas, invalidation tests).
   *
   * @param viewGroup The ViewGroup to be added to the TileView, that will scale visually.
   */
  public void addScalingViewGroup( ViewGroup viewGroup ) {
    mScalingLayout.addView( viewGroup );
  }

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
   *
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
  protected void requestSafeRender() {
    if( isFlinging() ) {
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
   * Notify the TileView that it should resume tiles rendering.
   */
  public void resumeRender() {
    mTileCanvasViewGroup.resumeRender();
  }

  /**
   * Sets a custom class to perform the getBitmap operation when tile bitmaps are requested for
   * tile images only.
   * By default, a BitmapDecoder implementation is provided that renders bitmaps from the context's
   * Assets, but alternative implementations could be used that fetch images via HTTP, or from the
   * SD card, or resources, SVG, etc.
   *
   * @param bitmapProvider A class instance that implements BitmapProvider, and must define a getBitmap method, which accepts a String file name and a Context object, and returns a Bitmap
   */
  public void setBitmapProvider( BitmapProvider bitmapProvider ) {
    mTileCanvasViewGroup.setBitmapProvider( bitmapProvider );
  }

  /**
   * Defines whether tile bitmaps should be rendered using an AlphaAnimation
   *
   * @param enabled True if the TileView should render tiles with fade transitions
   */
  public void setTransitionsEnabled( boolean enabled ) {
    mTileCanvasViewGroup.setTransitionsEnabled( enabled );
  }

  /**
   * Instructs Tile instances to recycle (or not).  This can be useful if using a caching system
   * that re-uses bitmaps and expects them to not have been recycled.
   *
   * The default value is true.
   *
   * @deprecated This value is no longer considered - bitmaps are always recycled when they're no longer used.
   * @param shouldRecycleBitmaps True if bitmaps should call Bitmap.recycle when they are removed from view.
   */
  public void setShouldRecycleBitmaps( boolean shouldRecycleBitmaps ) {
    mTileCanvasViewGroup.setShouldRecycleBitmaps( shouldRecycleBitmaps );
  }

  /**
   * Defines the total size, in pixels, of the tile set at 100% scale.
   * The TileView wills pan within it's layout dimensions, with the content (scrollable)
   * size defined by this method.
   *
   * @param width  Total width of the tiled set.
   * @param height Total height of the tiled set.
   */
  @Override
  public void setSize( int width, int height ) {
    super.setSize( width, height );
    mDetailLevelManager.setSize( width, height );
    mCoordinateTranslater.setSize( width, height );
  }

  /**
   * Register a tile set to be used for a particular detail level.
   * Each tile set to be used must be registered using this method,
   * and at least one tile set must be registered for the TileView to render any tiles.
   *
   * @param detailScale Scale at which the TileView should use the tiles in this set.
   * @param data        An arbitrary object of any type that is passed to the BitmapProvider for each tile on this level.
   */
  public void addDetailLevel( float detailScale, Object data ) {
    addDetailLevel( detailScale, data, DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE );
  }

  /**
   * Register a tile set to be used for a particular detail level.
   * Each tile set to be used must be registered using this method,
   * and at least one tile set must be registered for the TileView to render any tiles.
   *
   * @param detailScale Scale at which the TileView should use the tiles in this set.
   * @param data        An arbitrary object of any type that is passed to the (Adapter|Decoder) for each tile on this level.
   * @param tileWidth   Size of each tiled column.
   * @param tileHeight  Size of each tiled row.
   */
  public void addDetailLevel( float detailScale, Object data, int tileWidth, int tileHeight ) {
    mDetailLevelManager.addDetailLevel( detailScale, data, tileWidth, tileHeight );
  }

  /**
   * Pads the viewport by the number of pixels passed.  e.g., setViewportPadding( 100 ) instructs the
   * TileView to interpret it's actual viewport offset by 100 pixels in each direction (top, left,
   * right, bottom), so more tiles will qualify for "visible" status when intersections are calculated.
   *
   * @param padding The number of pixels to pad the viewport by
   */
  public void setViewportPadding( int padding ) {
    mDetailLevelManager.setViewportPadding( padding );
  }

  /**
   * Register a set of offset points to use when calculating position within the TileView.
   * Any type of coordinate system can be used (any type of lat/lng, percentile-based, etc),
   * and all positioned are calculated relatively.  If relative bounds are defined, position parameters
   * received by TileView methods will be translated to the the appropriate pixel value.
   * To remove this process, use undefineBounds.
   *
   * @param left   The left edge of the rectangle used when calculating position.
   * @param top    The top edge of the rectangle used when calculating position.
   * @param right  The right edge of the rectangle used when calculating position.
   * @param bottom The bottom edge of the rectangle used when calculating position.
   */
  public void defineBounds( double left, double top, double right, double bottom ) {
    mCoordinateTranslater.setBounds( left, top, right, bottom );
  }

  /**
   * Unregisters arbitrary bounds and coordinate system.  After invoking this method,
   * TileView methods that receive position method parameters will use pixel values,
   * relative to the TileView's registered size (at 1.0f scale).
   */
  public void undefineBounds() {
    mCoordinateTranslater.unsetBounds();
  }

  /**
   * Scrolls (instantly) the TileView to the x and y positions provided.  The is an overload
   * of scrollTo( int x, int y ) that accepts doubles; if the TileView has relative bounds defined,
   * those relative doubles will be converted to absolute pixel positions.
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
   * Scrolls (instantly) the TileView to the x and y positions provided,
   * then centers the viewport to the position.
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
   * Scrolls (with animation) the TileView to the x and y positions provided,
   * then centers the viewport to the position.
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

  /**
   * Scrolls and scales (with animation) the TileView to the specified x, y and scale provided.
   * The TileView will be centered to the coordinates passed.
   *
   * @param x     The relative x position to move to.
   * @param y     The relative y position to move to.
   * @param scale The scale the TileView should be at when the animation is complete.
   */
  public void slideToAndCenterWithScale( double x, double y, float scale ) {
    slideToAndCenterWithScale(
      mCoordinateTranslater.translateAndScaleX( x, scale ),
      mCoordinateTranslater.translateAndScaleY( y, scale ),
      scale
    );
  }

  /**
   * Markers added to this TileView will have anchor logic applied on the values provided here.
   * E.g., setMarkerAnchorPoints(-0.5f, -1.0f) will have markers centered horizontally, and aligned
   * along the bottom edge to the y value supplied.
   *
   * Anchor values assigned to individual markers will override these default values.
   *
   * @param anchorX The x-axis position of a marker will be offset by a number equal to the width of the marker multiplied by this value.
   * @param anchorY The y-axis position of a marker will be offset by a number equal to the height of the marker multiplied by this value.
   */
  public void setMarkerAnchorPoints( Float anchorX, Float anchorY ) {
    mMarkerLayout.setAnchors( anchorX, anchorY );
  }

  /**
   * Add a marker to the the TileView.  The marker can be any View.
   * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters.
   *
   * @param view    View instance to be added to the TileView.
   * @param x       Relative x position the View instance should be positioned at.
   * @param y       Relative y position the View instance should be positioned at.
   * @param anchorX The x-axis position of a marker will be offset by a number equal to the width of the marker multiplied by this value.
   * @param anchorY The y-axis position of a marker will be offset by a number equal to the height of the marker multiplied by this value.
   * @return The View instance added to the TileView.
   */
  public View addMarker( View view, double x, double y, Float anchorX, Float anchorY ) {
    return mMarkerLayout.addMarker( view,
      mCoordinateTranslater.translateX( x ),
      mCoordinateTranslater.translateY( y ),
      anchorX, anchorY
    );
  }

  /**
   * Removes a marker View from the TileView's view tree.
   *
   * @param view The marker View to be removed.
   */
  public void removeMarker( View view ) {
    mMarkerLayout.removeMarker( view );
  }

  /**
   * Moves an existing marker to another position.
   *
   * @param view The marker View to be repositioned.
   * @param x    Relative x position the View instance should be positioned at.
   * @param y    Relative y position the View instance should be positioned at.
   */
  public void moveMarker( View view, double x, double y ) {
    mMarkerLayout.moveMarker( view,
      mCoordinateTranslater.translateX( x ),
      mCoordinateTranslater.translateY( y ) );
  }

  /**
   * Scroll the TileView so that the View passed is centered in the viewport.
   *
   * @param view          The View marker that the TileView should center on.
   * @param shouldAnimate True if the movement should use a transition effect.
   */
  public void moveToMarker( View view, boolean shouldAnimate ) {
    if( mMarkerLayout.indexOfChild( view ) == -1 ) {
      throw new IllegalStateException( "The view passed is not an existing marker" );
    }
    ViewGroup.LayoutParams params = view.getLayoutParams();
    if( params instanceof MarkerLayout.LayoutParams ) {
      MarkerLayout.LayoutParams anchorLayoutParams = (MarkerLayout.LayoutParams) params;
      int scaledX = FloatMathHelper.scale( anchorLayoutParams.x, getScale() );
      int scaledY = FloatMathHelper.scale( anchorLayoutParams.y, getScale() );
      if( shouldAnimate ) {
        slideToAndCenter( scaledX, scaledY );
      } else {
        scrollToAndCenter( scaledX, scaledY );
      }
    }
  }

  /**
   * Register a MarkerTapListener for the TileView instance (rather than on a single marker view).
   * Unlike standard touch events attached to marker View's (e.g., View.OnClickListener),
   * MarkerTapListener.onMarkerTapEvent does not consume the touch event, so will not interfere
   * with scrolling.
   *
   * @param markerTapListener Listener to be added to the TileView's list of MarkerTapListener.
   */
  public void setMarkerTapListener( MarkerLayout.MarkerTapListener markerTapListener ) {
    mMarkerLayout.setMarkerTapListener( markerTapListener );
  }

  /**
   * Add a callout to the the TileView.  The callout can be any View.
   * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both
   * width and height, and positioned according to the x and y values supplied.
   * Callout views will always be positioned at the top of the view tree (at the highest z-index),
   * and will always be removed during any touch event that is not consumed by the callout View.
   *
   * @param view    View instance to be added to the TileView.
   * @param x       Relative x position the View instance should be positioned at.
   * @param y       Relative y position the View instance should be positioned at.
   * @param anchorX The x-axis position of a callout view will be offset by a number equal to the width of the callout view multiplied by this value.
   * @param anchorY The y-axis position of a callout view will be offset by a number equal to the height of the callout view multiplied by this value.
   * @return The View instance added to the TileView.
   */
  public View addCallout( View view, double x, double y, Float anchorX, Float anchorY ) {
    return mCalloutLayout.addMarker( view,
      mCoordinateTranslater.translateX( x ),
      mCoordinateTranslater.translateY( y ),
      anchorX, anchorY
    );
  }

  /**
   * Removes a callout View from the TileView.
   *
   * @param view The callout View to be removed.
   */
  public void removeCallout( View view ) {
    mCalloutLayout.removeMarker( view );
  }

  /**
   * Register a HotSpot that should fire a listener when a touch event occurs that intersects the
   * Region defined by the HotSpot.
   *
   * The HotSpot virtually moves and scales with the TileView.
   *
   * @param hotSpot The hotspot that is tested against touch events that occur on the TileView.
   * @return The HotSpot instance added.
   */
  public HotSpot addHotSpot( HotSpot hotSpot ) {
    mHotSpotManager.addHotSpot( hotSpot );
    return hotSpot;
  }

  /**
   * Register a HotSpot that should fire a listener when a touch event occurs that intersects the
   * Region defined by the HotSpot.
   *
   * The HotSpot virtually moves and scales with the TileView.
   *
   * @param positions (List<double[]>) List of paired doubles that represents the region.
   * @return HotSpot the hotspot created with this method.
   */
  public HotSpot addHotSpot( List<double[]> positions, HotSpot.HotSpotTapListener listener ) {
    Path path = mCoordinateTranslater.pathFromPositions( positions, true );
    RectF bounds = new RectF();
    path.computeBounds( bounds, true );
    Rect rect = new Rect();
    bounds.round( rect );
    Region clip = new Region( rect );
    HotSpot hotSpot = new HotSpot();
    hotSpot.setPath( path, clip );
    hotSpot.setHotSpotTapListener( listener );
    return addHotSpot( hotSpot );
  }

  /**
   * Remove a HotSpot registered with addHotSpot.
   *
   * @param hotSpot The HotSpot instance to remove.
   */
  public void removeHotSpot( HotSpot hotSpot ) {
    mHotSpotManager.removeHotSpot( hotSpot );
  }

  /**
   * Register a HotSpotTapListener with the TileView.  This listener will fire if any registered
   * HotSpot's region intersects a Tap event.
   *
   * @param hotSpotTapListener The listener to be added.
   */
  public void setHotSpotTapListener( HotSpot.HotSpotTapListener hotSpotTapListener ) {
    mHotSpotManager.setHotSpotTapListener( hotSpotTapListener );
  }

  /**
   * Register a DrawablePath that will be drawn on a layer above the tiles, but below markers.
   * The Path will be scaled with the TileView, but will always be as wide as the stroke set
   * for the Paint instance associated with the DrawablePath.
   *
   * @param drawablePath DrawablePath instance to be drawn by the TileView.
   * @return The DrawablePath instance passed to the TileView.
   */
  public CompositePathView.DrawablePath drawPath( CompositePathView.DrawablePath drawablePath ) {
    return mCompositePathView.addPath( drawablePath );
  }

  /**
   * Register a Path and Paint that will be drawn on a layer above the tiles, but below markers.
   * The Path will be scaled with the TileView, but will always be as wide as the stroke set
   * for the Paint.
   *
   * @param positions List of doubles that represent the points of the Path.
   * @param paint     The Paint instance that defines the style of the drawn path.
   * @return The DrawablePath instance passed to the TileView.
   */
  public CompositePathView.DrawablePath drawPath( List<double[]> positions, Paint paint ) {
    Path path = mCoordinateTranslater.pathFromPositions( positions, false );
    return mCompositePathView.addPath( path, paint );
  }

  /**
   * Removes a DrawablePath from the TileView's registry.  This path will no longer be drawn by the
   * TileView.
   *
   * @param drawablePath The DrawablePath instance to be removed.
   */
  public void removePath( CompositePathView.DrawablePath drawablePath ) {
    mCompositePathView.removePath( drawablePath );
  }

  /**
   * Returns the Paint instance used by the CompositePathView by default.  This can be modified for
   * future Path paint operations.
   *
   * @return The Paint instance used by default.
   */
  public Paint getDefaultPathPaint() {
    return mCompositePathView.getDefaultPaint();
  }

  /**
   * Recycles bitmap image files, prevents path drawing, and clears pending Handler messages,
   * appropriate for Activity.onPause.
   */
  public void pause() {
    mRenderThrottleHandler.clear();
    mDetailLevelManager.invalidateAll();
    setWillNotDraw( true );
  }

  /**
   * Clear tile image files and remove all views, appropriate for Activity.onDestroy.
   * After invoking this method, the TileView instance should be removed from any view trees,
   * and references to it should be set to null.
   */
  public void destroy() {
    pause();
    mTileCanvasViewGroup.destroy();
    mCompositePathView.clear();
    removeAllViews();
  }

  /**
   * Restore visible state (generally after a call to pause).
   * Appropriate for Activity.onResume.
   */
  public void resume() {
    setWillNotDraw( false );
    updateViewport();
    mTileCanvasViewGroup.updateTileSet( mDetailLevelManager.getCurrentDetailLevel() );
    requestRender();
    requestLayout();
  }

  /**
   * Allows the TileView to render tiles while panning.
   *
   * @param shouldRender True if it should render while panning.
   */
  public void setShouldRenderWhilePanning( boolean shouldRender ) {
    mShouldRenderWhilePanning = shouldRender;
    int buffer = shouldRender ? TileCanvasViewGroup.FAST_RENDER_BUFFER : TileCanvasViewGroup.DEFAULT_RENDER_BUFFER;
    mTileCanvasViewGroup.setRenderBuffer( buffer );
  }

  /**
   * By default, when a zoom begins, the current {@link DetailLevel} is locked so it is used to
   * provide tiles until the zoom ends. This ensures that the {@link TileView} is updated
   * consistently.
   * <p>
   * However, a zoom out may require a lot of tiles of the locked {@code DetailLevel} to be rendered.
   * In worst case, it can cause {@link OutOfMemoryError}.
   * Then, disabling the {@code DetailLevel} lock is a bandage to that issue. Using
   * {@code setShouldUpdateDetailLevelWhileZooming( true )} is not advised unless you have that issue.
   * </p>
   *
   * @param shouldUpdate True if it should lock {@link DetailLevel} when a zoom begins.
   */
  public void setShouldUpdateDetailLevelWhileZooming( boolean shouldUpdate ) {
    mShouldUpdateDetailLevelWhileZooming = shouldUpdate;
  }

  /**
   * Allows the use of a custom {@link DetailLevelManager}.
   * <p>
   * For example, to change the logic of {@link DetailLevel} choice for a given scale, you
   * declare your own {@code DetailLevelMangerCustom} that extends {@link DetailLevelManager} :
   * <pre>{@code
   * private class DetailLevelManagerCustom extends DetailLevelManager{
   *  @literal @Override
   *   public DetailLevel getDetailLevelForScale(){
   *     // your logic here
   *   }
   * }
   * }
   * </pre>
   * Then you should use {@code TileView.setDetailLevelManager} before other method calls, especially
   * {@code TileView.setSize} and {@code TileView.addDetailLevel}.
   * </p>
   *
   * @param manager The DetailLevelManager instance used.
   */
  public void setDetailLevelManager( DetailLevelManager manager ) {
    mDetailLevelManager = manager;
  }

  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {
    super.onLayout( changed, l, t, r, b );
    updateViewport();
    requestRender();
  }

  protected void updateViewport() {
    int left = getScrollX();
    int top = getScrollY();
    int right = left + getWidth();
    int bottom = top + getHeight();
    mDetailLevelManager.updateViewport( left, top, right, bottom );
  }

  @Override
  protected void onScrollChanged( int l, int t, int oldl, int oldt ) {
    super.onScrollChanged( l, t, oldl, oldt );
    updateViewport();
    if( mShouldRenderWhilePanning ) {
      requestRender();
    } else {
      requestThrottledRender();
    }
  }

  @Override
  public void onScaleChanged( float scale, float previous ) {
    super.onScaleChanged( scale, previous );
    mDetailLevelManager.setScale( scale );
    mHotSpotManager.setScale( scale );
    mTileCanvasViewGroup.setScale( scale );
    mScalingLayout.setScale( scale );
    mCompositePathView.setScale( scale );
    mMarkerLayout.setScale( scale );
    mCalloutLayout.setScale( scale );
  }

  @Override
  public void onPanBegin( int x, int y, Origination origin ) {

  }

  @Override
  public void onPanUpdate( int x, int y, Origination origin ) {

  }

  @Override
  public void onPanEnd( int x, int y, Origination origin ) {
    requestRender();
  }

  @Override
  public void onZoomBegin( float scale, Origination origin ) {
    if ( origin == null ) {
      mTileCanvasViewGroup.suppressRender();
    }
    mDetailLevelManager.setScale( scale );
  }

  @Override
  public void onZoomUpdate( float scale, Origination origin ) {

  }

  @Override
  public void onZoomEnd( float scale, Origination origin ) {
    if ( origin == null ) {
      mTileCanvasViewGroup.resumeRender();
    }
    mDetailLevelManager.setScale( scale );
    requestRender();
  }

  @Override
  public void onDetailLevelChanged( DetailLevel detailLevel ) {
    requestRender();
    mTileCanvasViewGroup.updateTileSet( detailLevel );
  }

  @Override
  public boolean onSingleTapConfirmed( MotionEvent event ) {
    int x = (int) (getScrollX() + event.getX());
    int y = (int) (getScrollY() + event.getY());
    mMarkerLayout.processHit( x, y );
    mHotSpotManager.processHit( x, y );
    return super.onSingleTapConfirmed( event );
  }

  @Override
  public void onRenderStart() {

  }

  @Override
  public void onRenderCancelled() {

  }

  @Override
  public void onRenderComplete() {

  }

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

  /**
   * Object used to keep some data when a configuration change happens and the activity is
   * re-created.
   * It's boiler-plate but this is how to save View state.
   */
  private static class SavedState extends BaseSavedState {
    /* This will store the current scale and position */
    float mScale;
    int mSavedCenterX;
    int mSavedCenterY;

    SavedState( Parcelable superState ) {
      super( superState );
    }

    private SavedState( Parcel in ) {
      super( in );
      mScale = in.readFloat();
      mSavedCenterX = in.readInt();
      mSavedCenterY = in.readInt();
    }

    @Override
    public void writeToParcel( Parcel out, int flags ) {
      super.writeToParcel( out, flags );
      out.writeFloat( mScale );
      out.writeInt( mSavedCenterX );
      out.writeInt( mSavedCenterY );
    }

    public static final Parcelable.Creator<SavedState> CREATOR
            = new Parcelable.Creator<SavedState>() {
      public SavedState createFromParcel( Parcel in ) {
        return new SavedState( in );
      }

      public SavedState[] newArray( int size ) {
        return new SavedState[ size ];
      }
    };
  }

  /**
   * The default {@code super.onSaveInstanceState} and {@code onRestoreInstanceState} don't
   * restore the position on the map as expected (if the instance of {@link TileView} remains the
   * same). For this reason and if a new {@link TileView} instance is created, we have to save
   * the current scale and position on the map, to restore them later when the {@link TileView} is
   * recreated.
   */
  @Override
  public Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    SavedState ss = new SavedState( superState );
    ss.mScale = getScale();
    ss.mSavedCenterX = getScrollX() + getHalfWidth();
    ss.mSavedCenterY = getScrollY() + getHalfHeight();
    return ss;
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {
    final SavedState ss = (SavedState) state;
    super.onRestoreInstanceState( ss.getSuperState() );
    setScale( ss.mScale );
    post(new Runnable() {
      @Override
      public void run() {
        scrollToAndCenter(ss.mSavedCenterX, ss.mSavedCenterY);
      }
    });
  }

}