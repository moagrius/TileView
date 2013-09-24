package com.qozix.tileview;

import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.view.View;

import com.qozix.layouts.ZoomPanLayout;
import com.qozix.tileview.detail.DetailLevelEventListener;
import com.qozix.tileview.detail.DetailLevelPatternParser;
import com.qozix.tileview.detail.DetailManager;
import com.qozix.tileview.geom.PositionManager;
import com.qozix.tileview.graphics.BitmapDecoder;
import com.qozix.tileview.graphics.BitmapDecoderHttp;
import com.qozix.tileview.hotspots.HotSpot;
import com.qozix.tileview.hotspots.HotSpotEventListener;
import com.qozix.tileview.hotspots.HotSpotManager;
import com.qozix.tileview.markers.CalloutManager;
import com.qozix.tileview.markers.MarkerEventListener;
import com.qozix.tileview.markers.MarkerManager;
import com.qozix.tileview.paths.DrawablePath;
import com.qozix.tileview.paths.PathHelper;
import com.qozix.tileview.paths.PathManager;
import com.qozix.tileview.samples.SampleManager;
import com.qozix.tileview.tiles.TileManager;
import com.qozix.tileview.tiles.TileRenderListener;

/**
 * The TileView widget is a subclass of ViewGroup that provides a mechanism to asynchronously display tile-based images,
 * with additional functionality for 2D dragging, flinging, pinch or double-tap to zoom, adding overlaying Views (markers),
 * built-in Hot Spot support, dynamic path drawing, multiple levels of detail, and support for any relative positioning or 
 * coordinate system.
 * 
 * <p>A minimal implementation:</p>
 *  
 * <pre>{@code
 * TileView tileView = new TileView(this);
 * tileView.setSize(3000,5000);
 * tileView.addDetailLevel(1.0f, "path/to/tiles/%col%-%row%.jpg");
 * }</pre>
 * 
 * A more advanced implementation might look like:
 * <pre>{@code
 * TileView tileView = new TileView(this);
 * tileView.setSize(3000,5000);
 * tileView.addTileViewEventListener(someMapEventListener);
 * tileView.defineRelativeBounds(42.379676, -71.094919, 42.346550, -71.040280);
 * tileView.addDetailLevel(1.000f, "tiles/boston-1000-%col%_%row%.jpg", 256, 256);
 * tileView.addDetailLevel(0.500f, "tiles/boston-500-%col%_%row%.jpg", 256, 256);
 * tileView.addDetailLevel(0.250f, "tiles/boston-250-%col%_%row%.jpg", 256, 256);
 * tileView.addDetailLevel(0.125f, "tiles/boston-125-%col%_%row%.jpg", 128, 128);
 * tileView.addMarker(someView, 42.35848, -71.063736);
 * tileView.addMarker(anotherView, 42.3665, -71.05224);
 * tileView.addMarkerEventListener(someMarkerEventListener);
 * }</pre>
 * 
 */
public class TileView extends ZoomPanLayout
{
    private final HashSet<TileViewEventListener> tileViewEventListeners = new HashSet<TileViewEventListener>();

    private final DetailManager                  detailManager          = new DetailManager();
    private final PositionManager                positionManager        = new PositionManager();

    private final HotSpotManager                 hotSpotManager         = new HotSpotManager(this.detailManager);

    private final SampleManager                  sampleManager;
    private final TileManager                    tileManager;
    private final PathManager                    pathManager;
    private final MarkerManager                  markerManager;
    private final CalloutManager                 calloutManager;

    /**
     * Constructor to use when creating a TileView from code.  Inflating from XML is not currently supported.
     * @param context (Context) The Context the TileView is running in, through which it can access the current theme, resources, etc.
     */
    public TileView(final Context context)
    {
        super(context);

        this.sampleManager = new SampleManager(context, this.detailManager);
        this.addView(this.sampleManager);

        this.tileManager = new TileManager(context, this.detailManager);
        this.addView(this.tileManager);

        this.pathManager = new PathManager(context, this.detailManager);
        this.addView(this.pathManager);

        this.markerManager = new MarkerManager(context, this.detailManager);
        this.addView(this.markerManager);

        this.calloutManager = new CalloutManager(context, this.detailManager);
        this.addView(this.calloutManager);

        this.detailManager.addDetailLevelEventListener(this.detailLevelEventListener);
        this.tileManager.setTileRenderListener(this.renderListener);

        this.addZoomPanListener(this.zoomPanListener);
        this.addGestureListener(this.gestureListener);

        this.requestRender();

    }

    //------------------------------------------------------------------------------------
    // PUBLIC API
    //------------------------------------------------------------------------------------

    //------------------------------------------------------------------------------------
    // Event Management API
    //------------------------------------------------------------------------------------

    /**
     * Register an event listener callback object for this TileView.
     * Note this is method adds a listener to an array of listeners, and does not set
     * a single listener member a single listener.
     * @param listener (TileViewEventListener) an implementation of the TileViewEventListener interface
     */
    public void addTileViewEventListener(final TileViewEventListener listener)
    {
        this.tileViewEventListeners.add(listener);
    }

    /**
     * Removes a TileViewEventListener object from those listening to this TileView.
     * @param listener (TileViewEventListener) an implementation of the TileViewEventListener interface
     */
    public void removeTileViewEventListener(final TileViewEventListener listener)
    {
        this.tileViewEventListeners.remove(listener);
    }

    //------------------------------------------------------------------------------------
    // Rendering API
    //------------------------------------------------------------------------------------

    /**
     * Request that the current tile set is re-examined and re-drawn.
     * The request is added to a queue and is not guaranteed to be processed at any particular
     * time, and will never be handled immediately.
     */
    public void requestRender()
    {
        this.tileManager.requestRender();
    }

    /**
     * Notify the TileView that it may stop rendering tiles.  The rendering thread will be
     * sent an interrupt request, but no guarantee is provided when the request will be responded to.
     */
    public void cancelRender()
    {
        this.tileManager.cancelRender();
    }

    /**
     * Enables or disables tile image caching (in-memory and on-disk)
     * @param shouldCache (boolean) true to enable caching, false to disable it (default)
     */
    public void setCacheEnabled(final boolean shouldCache)
    {
        this.tileManager.setCacheEnabled(shouldCache);
    }

    /**
     * Sets a custom class to perform the decode operation when tile bitmaps are requested.
     * By default, a BitmapDecoder implementation is provided that renders bitmaps from the context's Assets,
     * but alternative implementations could be used that fetch images via HTTP, or from the SD card, or resources, SVG, etc.
     * This signature is identical to calling setTileDecoder and setDownsampleDecoder with the same decoder instance as the parameter.
     * {@link BitmapDecoderHttp} is an example of such an implementation.
     * @param decoder (BitmapDecoder) A class instance that implements BitmapDecoder, and must define a decode method, which accepts a String file name and a Context object, and returns a Bitmap
     */
    public void setDecoder(final BitmapDecoder decoder)
    {
        this.setTileDecoder(decoder);
        this.setDownsampleDecoder(decoder);
    }

    /**
     * Sets a custom class to perform the decode operation when tile bitmaps are requested for tile images only.
     * By default, a BitmapDecoder implementation is provided that renders bitmaps from the context's Assets,
     * but alternative implementations could be used that fetch images via HTTP, or from the SD card, or resources, SVG, etc.
     * {@link BitmapDecoderHttp} is an example of such an implementation.
     * @param decoder (BitmapDecoder) A class instance that implements BitmapDecoder, and must define a decode method, which accepts a String file name and a Context object, and returns a Bitmap
     */
    public void setTileDecoder(final BitmapDecoder decoder)
    {
        this.tileManager.setDecoder(decoder);
    }

    /**
     * Sets a custom class to perform the decode operation when tile bitmaps are requested for downsample images only.
     * By default, a BitmapDecoder implementation is provided that renders bitmaps from the context's Assets,
     * but alternative implementations could be used that fetch images via HTTP, or from the SD card, or resources, SVG, etc.
     * {@link BitmapDecoderHttp} is an example of such an implementation.
     * @param decoder (BitmapDecoder) A class instance that implements BitmapDecoder, and must define a decode method, which accepts a String file name and a Context object, and returns a Bitmap
     */
    public void setDownsampleDecoder(final BitmapDecoder decoder)
    {
        this.sampleManager.setDecoder(decoder);
    }

    /**
     * Defines whether tile bitmaps should be rendered using an AlphaAnimation
     * @param enabled (boolean) true if the TileView should render tiles with fade transitions
     */
    public void setTransitionsEnabled(final boolean enabled)
    {
        this.tileManager.setTransitionsEnabled(enabled);
    }

    /**
     * Define the duration (in milliseconds) for each tile transition.
     * @param duration (int) the duration of the transition in milliseconds.
     */
    public void setTransitionDuration(final int duration)
    {
        this.tileManager.setTransitionDuration(duration);
    }

    //------------------------------------------------------------------------------------
    // Detail Level Management API
    //------------------------------------------------------------------------------------

    /**
     * Defines the total size, in pixels, of the tile set at 100% scale.
     * The TileView wills pan within it's layout dimensions, with the content (scrollable)
     * size defined by this method.
     * @param width (int) total width of the tiled set
     * @param height (int) total height of the tiled set
     */
    @Override
    public void setSize(final int w, final int h)
    {
        // super (define clip area)
        super.setSize(w, h);
        // coordinate with other components
        this.detailManager.setSize(w, h);
        // notify manager for relative positioning
        this.positionManager.setSize(w, h);
    }

    /**
     * Register a tile set to be used for a particular detail level.
     * Each tile set to be used must be registered using this method,
     * and at least one tile set must be registered for the TileView to render any tiles.
     * @param detailScale (float) scale at which the TileView should use the tiles in this set.
     * @param pattern (String) string path to the location of the tile image files, to be parsed by a DetailLevelPatternParser
     */
    public void addDetailLevel(final float detailScale, final String pattern)
    {
        this.detailManager.addDetailLevel(detailScale, pattern, null);
    }

    /**
     * Register a tile set to be used for a particular detail level.
     * Each tile set to be used must be registered using this method,
     * and at least one tile set must be registered for the TileView to render any tiles.
     * @param detailScale (float) scale at which the TileView should use the tiles in this set.
     * @param pattern (String) string path to the location of the tile image files, to be parsed by a DetailLevelPatternParser
     * @param downsample (String) string path to the location of an optional non-tiled single image file that will fill the tile view, on a z-layer below tiles
     */
    public void addDetailLevel(final float detailScale, final String pattern, final String downsample)
    {
        this.detailManager.addDetailLevel(detailScale, pattern, downsample);
    }

    /**
     * Register a tile set to be used for a particular detail level.
     * Each tile set to be used must be registered using this method,
     * and at least one tile set must be registered for the TileView to render any tiles.
     * @param detailScale (float) scale at which the TileView should use the tiles in this set.
     * @param pattern (String) string path to the location of the tile image files, to be parsed by a DetailLevelPatternParser
     * @param downsample (String) string path to the location of an optional non-tiled single image file that will fill the tile view, on a z-layer below tiles
     * @param tileWidth (int) size of each tiled column
     * @param tileHeight (int) size of each tiled row
     */
    public void addDetailLevel(final float detailScale, final String pattern, final String downsample,
                    final int tileWidth, final int tileHeight)
    {
        this.detailManager.addDetailLevel(detailScale, pattern, downsample, tileWidth, tileHeight);
    }

    /**
     * Register a tile set to be used for a particular detail level.
     * Each tile set to be used must be registered using this method,
     * and at least one tile set must be registered for the TileView to render any tiles.
     * @param detailScale (float) scale at which the TileView should use the tiles in this set.
     * @param pattern (String) string path to the location of the tile image files, to be parsed by a DetailLevelPatternParser
     * @param downsample (String) string path to the location of an optional non-tiled single image file that will fill the tile view, on a z-layer below tiles
     * @param tileWidth (int) size of each tiled column
     * @param tileHeight (int) size of each tiled row
     * @param scaleMin (double) minimum scale at which to use these tiles
     * @param scaleMax (double) maximum scale at which to use these tiles
     */
    public void addDetailLevel(final float detailScale, final String pattern, final String downsample,
                    final int tileWidth, final int tileHeight, final double scaleMin, final double scaleMax)
    {
        this.detailManager.addDetailLevel(detailScale, pattern, downsample, tileWidth, tileHeight, scaleMin, scaleMax);
    }

    /**
     * Clear all previously registered zoom levels.  This method is experimental.
     */
    public void resetDetailLevels()
    {
        this.detailManager.resetDetailLevels();
        this.refresh();
    }

    /**
     * While the detail level is locked (after this method is invoked, and before unlockDetailLevel is invoked),
     * the DetailLevel will not change, and the current DetailLevel will be scaled beyond the normal
     * bounds.  Normally, during any scale change the details manager searches for the DetailLevel with
     * a registered scale closest to the defined scale.  While locked, this does not occur.
     */
    public void lockDetailLevel()
    {
        this.detailManager.lockDetailLevel();
    }

    /**
     * Unlocks a DetailLevel locked with lockDetailLevel
     */
    public void unlockDetailLevel()
    {
        this.detailManager.unlockDetailLevel();
    }

    /**
     * pads the viewport by the number of pixels passed.  e.g., setViewportPadding( 100 ) instructs the
     * TileView to interpret it's actual viewport offset by 100 pixels in each direction (top, left,
     * right, bottom), so more tiles will qualify for "visible" status when intersections are calculated.
     * @param padding (int) the number of pixels to pad the viewport by
     */
    public void setViewportPadding(final int padding)
    {
        this.detailManager.setPadding(padding);
    }

    /**
     * Define a custom parser to manage String file names representing image tiles
     * @param parser (DetailLevelPatternParser) parser that returns String objects from passed pattern, column and row.
     */
    public void setTileSetPatternParser(final DetailLevelPatternParser parser)
    {
        this.detailManager.setDetailLevelPatternParser(parser);
    }

    //------------------------------------------------------------------------------------
    // Positioning API
    //------------------------------------------------------------------------------------

    /**
     * Register a set of offset points to use when calculating position within the TileView.
     * Any type of coordinate system can be used (any type of lat/lng, percentile-based, etc),
     * and all positioned are calculated relatively.  If relative bounds are defined, position parameters
     * received by TileView methods will be translated to the the appropriate pixel value.
     * To remove this process, use undefineRelativeBounds
     * @param left (double) the left edge of the rectangle used when calculating position (e.g, longitude of the bottom-right coordinate)
     * @param top (double) the top edge of the rectangle used when calculating position (e.g, latitude of the top-left coordinate)
     * @param right (double) the right edge of the rectangle used when calculating position (e.g, longitude of the top-left coordinate)
     * @param bottom (double) the bottom edge of the rectangle used when calculating position (e.g, latitude of the bottom-right coordinate)
     */
    public void defineRelativeBounds(final double left, final double top, final double right, final double bottom)
    {
        this.positionManager.setBounds(left, top, right, bottom);
    }

    /**
     * Unregisters arbitrary bounds and coordinate system.  After invoking this method, TileView methods that
     * receive position method parameters will use pixel values, relative to the TileView's registered size (at 1.0d scale)
     */
    public void undefineRelativeBounds()
    {
        this.positionManager.unsetBounds();
    }

    /**
     * Translate a relative x and y position into a Point object with x and y values populated as pixel values, relative to the size of the TileView.
     * @param x (int) relative x position to be translated to absolute pixel value
     * @param y (int) relative y position to be translated to absolute pixel value
     * @return Point a Point object with x and y values calculated from the relative Position x and y values
     */
    public Point translate(final double x, final double y)
    {
        return this.positionManager.translate(x, y);
    }

    /**
     * Translate a List of relative x and y positions (double array... { x, y }
     * into Point objects with x and y values populated as pixel values, relative to the size of the TileView.
     * @param positions (List<double[]>) List of 2-element double arrays to be translated to Points (pixel values).  The first double should represent the relative x value, the second is y
     * @return List<Point> List of Point objects with x and y values calculated from the corresponding x and y values
     */
    public List<Point> translate(final List<double[]> positions)
    {
        return this.positionManager.translate(positions);
    }

    /**
     * Divides a number by the current scale value, effectively flipping scaled values.  This can be useful when
     * determining a relative position or dimension from a real pixel value.
     * @param value (double) The number to be inversely scaled.
     * @return (double) The inversely scaled product.
     */
    public double unscale(final double value)
    {
        return value / this.getScale();
    }

    /**
     * Scrolls (instantly) the TileView to the x and y positions provided.
     * @param x (double) the relative x position to move to
     * @param y (double) the relative y position to move to
     */
    public void moveTo(final double x, final double y)
    {
        final Point point = this.positionManager.translate(x, y, this.getScale());
        this.scrollToPoint(point);
    }

    /**
     * Scrolls (instantly) the TileView to the x and y positions provided, then centers the viewport to the position.
     * @param x (double) the relative x position to move to
     * @param y (double) the relative y position to move to
     */
    public void moveToAndCenter(final double x, final double y)
    {
        final Point point = this.positionManager.translate(x, y, this.getScale());
        this.scrollToAndCenter(point);
    }

    /**
     * Scrolls (with animation) the TIelView to the relative x and y positions provided.
     * @param x (double) the relative x position to move to
     * @param y (double) the relative y position to move to
     */
    public void slideTo(final double x, final double y)
    {
        final Point point = this.positionManager.translate(x, y, this.getScale());
        this.slideToPoint(point);
    }

    /**
     * Scrolls (with animation) the TileView to the x and y positions provided, then centers the viewport to the position.
     * @param x (double) the relative x position to move to
     * @param y (double) the relative y position to move to
     */
    public void slideToAndCenter(final double x, final double y)
    {
        final Point point = this.positionManager.translate(x, y, this.getScale());
        this.slideToAndCenter(point);
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
     * @param anchorX (float) the x-axis position of a marker will be offset by a number equal to the width of the marker multiplied by this value 
     * @param anchorY (float) the y-axis position of a marker will be offset by a number equal to the height of the marker multiplied by this value
     */
    public void setMarkerAnchorPoints(final float anchorX, final float anchorY)
    {
        this.markerManager.setAnchors(anchorX, anchorY);
    }

    /**
     * Add a marker to the the TileView.  The marker can be any View.
     * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
     * @param view (View) View instance to be added to the TileView
     * @param x (double) relative x position the View instance should be positioned at
     * @param y (double) relative y position the View instance should be positioned at
     * @return (View) the View instance added to the TileView
     */
    public View addMarker(final View view, final double x, final double y)
    {
        final Point point = this.positionManager.translate(x, y);
        return this.markerManager.addMarker(view, point.x, point.y);
    }

    /**
     * Add a marker to the the TileView.  The marker can be any View.
     * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
     * @param view (View) View instance to be added to the TileView
     * @param x (double) relative x position the View instance should be positioned at
     * @param y (double) relative y position the View instance should be positioned at
     * @param aX (float) the x-axis position of a marker will be offset by a number equal to the width of the marker multiplied by this value 
     * @param aY (float) the y-axis position of a marker will be offset by a number equal to the height of the marker multiplied by this value
     * @return (View) the View instance added to the TileView
     */
    public View addMarker(final View view, final double x, final double y, final float anchorX, final float anchorY)
    {
        final Point point = this.positionManager.translate(x, y);
        return this.markerManager.addMarker(view, point.x, point.y, anchorX, anchorY);
    }

    /**
     * Removes a marker View from the TileView's view tree.
     * @param view The marker View to be removed.
     */
    public void removeMarker(final View view)
    {
        this.markerManager.removeMarker(view);
    }

    /**
     * Register a MarkerEventListener.  Unlike standard touch events attached to marker View's (e.g., View.OnClickListener),
     * MarkerEventListeners do not consume the touch event, so will not interfere with scrolling.  While the event is
     * dispatched from a Tap event, it's routed though a hit detection API to trigger the listener.
     * @param listener (MarkerEventListener) listener to be added to the TileView's list of MarkerEventListeners
     */
    public void addMarkerEventListener(final MarkerEventListener listener)
    {
        this.markerManager.addMarkerEventListener(listener);
    }

    /**
     * Removes a MarkerEventListener from the TileView's registry.
     * @param listener (MarkerEventListener) listener to be removed From the TileView's list of MarkerEventListeners
     */
    public void removeMarkerEventListener(final MarkerEventListener listener)
    {
        this.markerManager.removeMarkerEventListener(listener);
    }

    /**
     * Add a callout to the the TileView.  The callout can be any View.
     * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
     * Callout views will always be positioned at the top of the view tree (at the highest z-index), and will always be removed during any touch event
     * that is not consumed by the callout View.
     * @param view (View) View instance to be added to the TileView's
     * @param x (double) relative x position the View instance should be positioned at
     * @param y (double) relative y position the View instance should be positioned at
     * @return (View) the View instance added to the TileView's
     */
    public View addCallout(final View view, final double x, final double y)
    {
        final Point point = this.positionManager.translate(x, y);
        return this.calloutManager.addMarker(view, point.x, point.y);
    }

    /**
     * Add a callout to the the TileView.  The callout can be any View.
     * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters
     * Callout views will always be positioned at the top of the view tree (at the highest z-index), and will always be removed during any touch event
     * that is not consumed by the callout View.
     * @param view (View) View instance to be added to the TileView's
     * @param x (double) relative x position the View instance should be positioned at
     * @param y (double) relative y position the View instance should be positioned at
     * @param aX (float) the x-axis position of a callout view will be offset by a number equal to the width of the callout view multiplied by this value 
     * @param aY (float) the y-axis position of a callout view will be offset by a number equal to the height of the callout view multiplied by this value
     * @return (View) the View instance added to the TileView's
     */
    public View addCallout(final View view, final double x, final double y, final float anchorX, final float anchorY)
    {
        final Point point = this.positionManager.translate(x, y);
        return this.calloutManager.addMarker(view, point.x, point.y, anchorX, anchorY);
    }

    /**
     * Removes a callout View from the TileView's view tree.
     * @param view The callout View to be removed.
     * @return (boolean) true if the view was in the view tree and was removed, false if it was not in the view tree
     */
    public boolean removeCallout(final View view)
    {
        if (this.calloutManager.indexOfChild(view) > -1)
        {
            this.calloutManager.removeView(view);
            return true;
        }
        return false;
    }

    /**
     * Register a HotSpot that should fire an listener when a touch event occurs that intersects that rectangle.
     * The HotSpot moves and scales with the TileView.
     * @param hotSpot (HotSpot) the hotspot that is tested against touch events that occur on the TileView
     * @return HotSpot the hotspot created with this method
     */
    public HotSpot addHotSpot(final HotSpot hotSpot)
    {
        this.hotSpotManager.addHotSpot(hotSpot);
        return hotSpot;
    }

    /**
     * Register a HotSpot that should fire an listener when a touch event occurs that intersects that rectangle.
     * The HotSpot moves and scales with the TileView.
     * @param positions (List<double[]>) List of paired doubles { x, y } that represents the points that make up the region.
     * @return HotSpot the hotspot created with this method
     */
    public HotSpot addHotSpot(final List<double[]> positions)
    {
        final List<Point> points = this.positionManager.translate(positions);
        final Path path = PathHelper.pathFromPoints(points);
        path.close();
        final RectF bounds = new RectF();
        path.computeBounds(bounds, true);
        final Rect rect = new Rect();
        bounds.round(rect);
        final Region clip = new Region(rect);
        final HotSpot hotSpot = new HotSpot();
        hotSpot.setPath(path, clip);
        return this.addHotSpot(hotSpot);
    }

    /**
     * Register a HotSpot that should fire an listener when a touch event occurs that intersects that rectangle.
     * The HotSpot moves and scales with the TileView.
     * @param positions (List<double[]>) List of paired doubles { x, y } that represents the points that make up the region.
     * @param listener (HotSpotEventListener) listener to attach to this hotspot, which will be invoked if a Tap event is fired that intersects the hotspot's Region
     * @return HotSpot the hotspot created with this method
     */
    public HotSpot addHotSpot(final List<double[]> positions, final HotSpotEventListener listener)
    {
        final HotSpot hotSpot = this.addHotSpot(positions);
        hotSpot.setHotSpotEventListener(listener);
        return hotSpot;
    }

    /**
     * Remove a HotSpot registered with addHotSpot
     * @param hotSpot (HotSpot) the hotspot to remove
     * @return (boolean) true if a hotspot was removed, false if not
     */
    public void removeHotSpot(final HotSpot hotSpot)
    {
        this.hotSpotManager.removeHotSpot(hotSpot);
    }

    /**
     * Register a HotSpotEventListener with the TileView.  This listener will fire if any hotspot's region intersects a Tap event.
     * @param listener (HotSpotEventListener) the listener to be added.
     */
    public void addHotSpotEventListener(final HotSpotEventListener listener)
    {
        this.hotSpotManager.addHotSpotEventListener(listener);
    }

    /**
     * Remove a HotSpotEventListener from the TileView's registry.
     * @param listener (HotSpotEventListener) the listener to be removed
     */
    public void removeHotSpotEventListener(final HotSpotEventListener listener)
    {
        this.hotSpotManager.removeHotSpotEventListener(listener);
    }

    //------------------------------------------------------------------------------------
    // Path Drawing API
    //------------------------------------------------------------------------------------

    /**
     * Register a Path and Paint that will be drawn on a layer above the tiles, but below markers.
     * This Path's will be scaled with the TileView, but will always be as wide as the stroke set for the Paint.
     * @param drawablePath (DrawablePath) a DrawablePath instance to be drawn by the TileView
     * @return DrawablePath the DrawablePath instance passed to the TileView
     */
    public DrawablePath drawPath(final DrawablePath drawablePath)
    {
        return this.pathManager.addPath(drawablePath);
    }

    /**
     * Register a Path and Paint that will be drawn on a layer above the tiles, but below markers.
     * This Path's will be scaled with the TileView, but will always be as wide as the stroke set for the Paint.
     * @param positions (List<double[]>) List of doubles { x, y } that represent the points of the Path.
     * @return DrawablePath the DrawablePath instance passed to the TileView
     */
    public DrawablePath drawPath(final List<double[]> positions)
    {
        final List<Point> points = this.positionManager.translate(positions);
        return this.pathManager.addPath(points);
    }

    /**
     * Register a Path and Paint that will be drawn on a layer above the tiles, but below markers.
     * This Path's will be scaled with the TileView, but will always be as wide as the stroke set for the Paint.
     * @param positions (List<double[]>) List of doubles { x, y } that represent the points of the Path.
     * @param paint (Paint) the Paint instance that defines the style of the drawn path.
     * @return DrawablePath the DrawablePath instance passed to the TileView
     */
    public DrawablePath drawPath(final List<double[]> positions, final Paint paint)
    {
        final List<Point> points = this.positionManager.translate(positions);
        return this.pathManager.addPath(points, paint);
    }

    /**
     * Removes a DrawablePath from the TileView's registry.  This path will no longer be drawn by the TileView.
     * @param drawablePath (DrawablePath) the DrawablePath instance to be removed.
     */
    public void removePath(final DrawablePath drawablePath)
    {
        this.pathManager.removePath(drawablePath);
    }

    /**
     * Returns the Paint instance used by default.  This can be modified for future Path paint operations.
     * @return Paint the Paint instance used by default.
     */
    public Paint getPathPaint()
    {
        return this.pathManager.getPaint();
    }

    //------------------------------------------------------------------------------------
    // Memory Management API
    //------------------------------------------------------------------------------------

    /**
     * Clear bitmap image files, appropriate for Activity.onPause
     */
    public void clear()
    {
        this.tileManager.clear();
        this.sampleManager.clear();
        this.pathManager.setShouldDraw(false);
    }

    /**
     * Clear bitmap image files, appropriate for Activity.onPause (mirror for .clear)
     */
    public void pause()
    {
        this.clear();
    }

    /**
     * Clear tile image files and remove all views, appropriate for Activity.onDestroy
     * References to TileView should be set to null following invocations of this method.
     */
    public void destroy()
    {
        this.tileManager.clear();
        this.sampleManager.clear();
        this.pathManager.clear();
    }

    /**
     * Restore visible state (generally after a call to .clear()
     * Appropriate for Activity.onResume
     */
    public void resume()
    {
        this.tileManager.requestRender();
        this.sampleManager.update();
        this.pathManager.setShouldDraw(true);
    }

    /**
     * Request the TileView reevaluate tile sets, rendered tiles, samples, invalidates, etc
     */
    public void refresh()
    {
        this.tileManager.updateTileSet();
        this.tileManager.requestRender();
        this.sampleManager.update();
        this.redraw();
    }

    public void setTileSelectionMethod(final int method)
    {
        this.detailManager.setTileSelectionMethod(method);
    }

    //------------------------------------------------------------------------------------
    // PRIVATE API
    //------------------------------------------------------------------------------------

    // make sure we keep the viewport UTD, and if layout changes we'll need to recompute what tiles to show
    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b)
    {
        super.onLayout(changed, l, t, r, b);
        if (changed)
        {
            this.updateViewport();
            this.requestRender();
        }
    }

    // let the zoom manager know what tiles to show based on our position and dimensions
    private void updateViewport()
    {
        final int left = this.getScrollX();
        final int top = this.getScrollY();
        final int right = left + this.getWidth();
        final int bottom = top + this.getHeight();
        this.detailManager.updateViewport(left, top, right, bottom);
    }

    // tell the tile renderer to not start any more tasks, but it can continue with any that are already running
    private void suppressRender()
    {
        this.tileManager.suppressRender();
    }

    //------------------------------------------------------------------------------------
    // Private Listeners
    //------------------------------------------------------------------------------------

    private final ZoomPanListener          zoomPanListener          = new ZoomPanListener()
                                                                    {
                                                                        @Override
                                                                        public void onZoomPanEvent()
                                                                        {

                                                                        }

                                                                        @Override
                                                                        public void onScrollChanged(final int x,
                                                                                        final int y)
                                                                        {
                                                                            TileView.this.updateViewport();
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onScrollChanged(x, y);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onScaleChanged(final double scale)
                                                                        {
                                                                            TileView.this.detailManager.setScale(scale);
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onScaleChanged(scale);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onZoomStart(final double scale)
                                                                        {
                                                                            TileView.this.detailManager
                                                                                            .lockDetailLevel();
                                                                            TileView.this.detailManager.setScale(scale);
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onZoomStart(scale);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onZoomComplete(final double scale)
                                                                        {
                                                                            TileView.this.detailManager
                                                                                            .unlockDetailLevel();
                                                                            TileView.this.detailManager.setScale(scale);
                                                                            TileView.this.requestRender(); // put this here instead of gesture listener so we catch animations and pinches
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onZoomComplete(scale);
                                                                            }
                                                                        }
                                                                    };

    private final DetailLevelEventListener detailLevelEventListener = new DetailLevelEventListener()
                                                                    {
                                                                        @Override
                                                                        public void onDetailLevelChanged()
                                                                        {
                                                                            TileView.this.requestRender();
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onDetailLevelChanged();
                                                                            }
                                                                        }

                                                                        /*
                                                                         * do *not* update scale in response to changes in the zoom manager
                                                                         * transactions are one-way - set scale on TileView (ZoomPanLayout)
                                                                         * and pass those to DetailManager, which then distributes, manages
                                                                         * and notifies all other interested parties.
                                                                         */
                                                                        @Override
                                                                        public void onDetailScaleChanged(
                                                                                        final double scale)
                                                                        {

                                                                        }
                                                                    };

    private final GestureListener          gestureListener          = new GestureListener()
                                                                    {

                                                                        @Override
                                                                        public void onDoubleTap(final Point point)
                                                                        {
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onDoubleTap(point.x, point.y);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onDrag(final Point point)
                                                                        {
                                                                            TileView.this.suppressRender();
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onDrag(point.x, point.y);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onFingerDown(final Point point)
                                                                        {
                                                                            TileView.this.suppressRender();
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onFingerDown(point.x, point.y);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onFingerUp(final Point point)
                                                                        {
                                                                            if (!TileView.this.isFlinging())
                                                                            {
                                                                                TileView.this.requestRender();
                                                                            }
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onFingerUp(point.x, point.y);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onFling(final Point startPoint,
                                                                                        final Point finalPoint)
                                                                        {
                                                                            TileView.this.suppressRender();
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onFling(startPoint.x,
                                                                                                startPoint.y,
                                                                                                finalPoint.x,
                                                                                                finalPoint.y);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onFlingComplete(final Point point)
                                                                        {
                                                                            TileView.this.requestRender();
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onFlingComplete(point.x,
                                                                                                point.y);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onPinch(final Point point)
                                                                        {
                                                                            TileView.this.suppressRender();
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onPinch(point.x, point.y);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onPinchComplete(final Point point)
                                                                        {
                                                                            TileView.this.requestRender();
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onPinchComplete(point.x,
                                                                                                point.y);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onPinchStart(final Point point)
                                                                        {
                                                                            TileView.this.suppressRender();
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onPinchStart(point.x, point.y);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onTap(final Point point)
                                                                        {
                                                                            TileView.this.markerManager
                                                                                            .processHit(point);
                                                                            TileView.this.hotSpotManager
                                                                                            .processHit(point);
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onTap(point.x, point.y);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onScrollComplete(final Point point)
                                                                        {
                                                                            TileView.this.requestRender();
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onScrollChanged(point.x,
                                                                                                point.y);
                                                                            }
                                                                        }
                                                                    };

    private final TileRenderListener       renderListener           = new TileRenderListener()
                                                                    {
                                                                        @Override
                                                                        public void onRenderCancelled()
                                                                        {

                                                                        }

                                                                        @Override
                                                                        public void onRenderComplete()
                                                                        {
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onRenderComplete();
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onRenderStart()
                                                                        {
                                                                            for (final TileViewEventListener listener : TileView.this.tileViewEventListeners)
                                                                            {
                                                                                listener.onRenderStart();
                                                                            }
                                                                        }
                                                                    };

    //------------------------------------------------------------------------------------
    // Public static interfaces and classes
    //------------------------------------------------------------------------------------

    /**
     * Interface for implementations to receive TileView events.  This interface consolidates several disparate
     * listeners (Gestures, ZoomPan Events, TileView events) into a single unit for ease of use.
     */
    public static interface TileViewEventListener
    {
        /**
         * Fires when a ACTION_DOWN event is raised from the TileView
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        public void onFingerDown(int x, int y);

        /**
         * Fires when a ACTION_UP event is raised from the TileView
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        public void onFingerUp(int x, int y);

        /**
         * Fires while the TileView is being dragged
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        public void onDrag(int x, int y);

        /**
         * Fires when a user double-taps the TileView
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        public void onDoubleTap(int x, int y);

        /**
         * Fires when a user taps the TileView
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        public void onTap(int x, int y);

        /**
         * Fires while a user is pinching the TileView
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        public void onPinch(int x, int y);

        /**
         * Fires when a user starts a pinch action
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        public void onPinchStart(int x, int y);

        /**
         * Fires when a user completes a pinch action
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        public void onPinchComplete(int x, int y);

        /**
         * Fires when a user initiates a fling action
         * @param sx (int) the x position of the start of the fling
         * @param sy (int) the y position of the start of the fling
         * @param dx (int) the x position of the end of the fling
         * @param dy (int) the y position of the end of the fling
         */
        public void onFling(int sx, int sy, int dx, int dy);

        /**
         * Fires when a fling action has completed
         * @param x (int) the final x scroll position of the TileView after the fling
         * @param y (int) the final y scroll position of the TileView after the fling
         */
        public void onFlingComplete(int x, int y);

        /**
         * Fires when the TileView's scale has updated
         * @param scale (double) the new scale of the TileView (0-1)
         */
        public void onScaleChanged(double scale);

        /**
         * Fires when the TileView's scroll position has updated
         * @param x (int) the new x scroll position of the TileView
         * @param y (int) the new y scroll position of the TileView
         */
        public void onScrollChanged(int x, int y);

        /**
         * Fires when a zoom action starts (typically through a pinch of double-tap action,
         * or by programmatic animated zoom methods.
         * @param scale (double) the new scale of the TileView (0-1)
         */
        public void onZoomStart(double scale);

        /**
         * Fires when a zoom action ends (typically through a pinch of double-tap action,
         * or by programmatic animated zoom methods.
         * @param scale (double) the new scale of the TileView (0-1)
         */
        public void onZoomComplete(double scale);

        /**
         * Fires when the TileView should start using a new DetailLevel
         */
        public void onDetailLevelChanged();

        /**
         * Fires when the rendering thread has started to update the visible tiles.
         */
        public void onRenderStart();

        /**
         * Fires when the rendering thread has completed updating the visible tiles, but before cleanup
         */
        public void onRenderComplete();
    }

    /**
     * Convenience class that implements {@TileViewEventListener}
     */
    public static class TileViewEventListenerImplementation implements TileViewEventListener
    {

        /**
         * Fires when a ACTION_DOWN event is raised from the TileView
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        @Override
        public void onFingerDown(final int x, final int y)
        {

        }

        /**
         * Fires when a ACTION_UP event is raised from the TileView
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        @Override
        public void onFingerUp(final int x, final int y)
        {

        }

        /**
         * Fires while the TileView is being dragged
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        @Override
        public void onDrag(final int x, final int y)
        {

        }

        /**
         * Fires when a user double-taps the TileView
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        @Override
        public void onDoubleTap(final int x, final int y)
        {

        }

        /**
         * Fires when a user taps the TileView
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        @Override
        public void onTap(final int x, final int y)
        {

        }

        /**
         * Fires while a user is pinching the TileView
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        @Override
        public void onPinch(final int x, final int y)
        {

        }

        /**
         * Fires when a user starts a pinch action
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        @Override
        public void onPinchStart(final int x, final int y)
        {

        }

        /**
         * Fires when a user completes a pinch action
         * @param x (int) the x position of the event
         * @param y (int) the y position of the event
         */
        @Override
        public void onPinchComplete(final int x, final int y)
        {

        }

        /**
         * Fires when a user initiates a fling action
         * @param sx (int) the x position of the start of the fling
         * @param sy (int) the y position of the start of the fling
         * @param dx (int) the x position of the end of the fling
         * @param dy (int) the y position of the end of the fling
         */
        @Override
        public void onFling(final int sx, final int sy, final int dx, final int dy)
        {

        }

        /**
         * Fires when a fling action has completed
         * @param x (int) the final x scroll position of the TileView after the fling
         * @param y (int) the final y scroll position of the TileView after the fling
         */
        @Override
        public void onFlingComplete(final int x, final int y)
        {

        }

        /**
         * Fires when the TileView's scale has updated
         * @param scale (double) the new scale of the TileView (0-1)
         */
        @Override
        public void onScaleChanged(final double scale)
        {

        }

        /**
         * Fires when the TileView's scroll position has updated
         * @param x (int) the new x scroll position of the TileView
         * @param y (int) the new y scroll position of the TileView
         */
        @Override
        public void onScrollChanged(final int x, final int y)
        {

        }

        /**
         * Fires when a zoom action starts (typically through a pinch of double-tap action,
         * or by programmatic animated zoom methods.
         * @param scale (double) the new scale of the TileView (0-1)
         */
        @Override
        public void onZoomStart(final double scale)
        {

        }

        /**
         * Fires when a zoom action ends (typically through a pinch of double-tap action,
         * or by programmatic animated zoom methods.
         * @param scale (double) the new scale of the TileView (0-1)
         */
        @Override
        public void onZoomComplete(final double scale)
        {

        }

        /**
         * Fires when the TileView should start using a new DetailLevel
         * @param oldZoom (int) the zoom level the TileView was using before the change
         * @param currentZoom (int) the zoom level the TileView has changed to
         */
        @Override
        public void onDetailLevelChanged()
        {

        }

        /**
         * Fires when the rendering thread has started to update the visible tiles.
         */
        @Override
        public void onRenderStart()
        {

        }

        /**
         * Fires when the rendering thread has completed updating the visible tiles, but before cleanup
         */
        @Override
        public void onRenderComplete()
        {

        }

    }

}