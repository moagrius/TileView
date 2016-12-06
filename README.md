![Release Badge](https://img.shields.io/github/release/moagrius/TileView.svg)

#TileView
The TileView widget is a subclass of ViewGroup that provides a mechanism to asynchronously display tile-based images, with additional functionality for 2D dragging, flinging, pinch or double-tap to zoom, adding overlaying Views (markers), built-in Hot Spot support, dynamic path drawing, multiple levels of detail, and support for any relative positioning or coordinate system.

![Demo](https://cloud.githubusercontent.com/assets/701344/17538476/6933099e-5e6b-11e6-9e18-45e924c19c91.gif)
<br>
Properly configured, TileView can render tiles quickly enough be appear seamless.
[](https://cloud.githubusercontent.com/assets/701344/10954033/d20843bc-8310-11e5-83ad-4e062b9b1be0.gif)

##News
**08/07/16** 2.2 is released, and provides some much-needed improvements in how tiles are rendered - please consider upgrading, but be aware there are some minor potential breaking changes (that should not affect 99% of users).  See [2.2.0 release notes](https://github.com/moagrius/TileView/releases/tag/2.2.0)

**03/18/16** if you're using a version earlier than 2.1, there were significant performance gains realized with 2.1 so we'd advise you to start using the most recent version (2.1 or later) immediately.  The improvements made also make fast-render viable, so we'd also encourage you to try `TileView.setShouldRenderWhilePanning(true);` if you'd like more responsive tile rendering.

##Version 2.0

**Version 2.0 released 10.25.15**

Version 2 is a major version change and is not backwards compatible with 1.x versions.  The API has changed but will still be familiar to users of 1.x.

Note that the original version of this library was written in early 2011.  Version 2 is the first major upgrade, and aims to provide a simpler API to a more robust and performant codebase.

Major goals were:

1.  Optimize tiling.  Tiles are now simply POJOs that manage Bitmaps, and are no longer ImageView instances.
2.  Leverage the gestures framework.  In order to behave more consistently with other Android widgets, several (all?) framework-provided gesture detector classes are used in version 2.
3.  Leverage the threading framework; take advantage of multi-core devices and multi-threading more aggressively.  Threading is managed using a ThreadPoolExecutor.
4.  Simplify and expose.  The API provides fewer overloaded signatures, but public access to nearly all core classes.
5.  Defer caching to the user.  Built-in caching has been removed.  The user can supply their own (or a third party) caching mechanism using the BitmapProvider interface.
6.  General refactoring.  There are too many simplifications and optimization to mention, but each class and each method has been revisited.
7.  Hooks hooks hooks!  While pan and zoom events are broadcast using a familiar listener mechanism, and should be sufficient for most use-cases, public hooks exist for a large number of operations that can be overriden by subclasses for custom functionality.

##Change Log
(Only major and minor changes are tracked here, consult git history for patches)

**2.2** Rewrite of tile rendering strategy, again with the help of @peterLaurence.  Peak memory consumption should be reduced, and Tile render performance should be improved.

**2.1** Rewrite of threading strategy, thanks to @peterLaurence and @bnsantos.  Tile render performance is substantially improved.

###Documentation
Javadocs are [here](http://moagrius.github.io/TileView/index.html?com/qozix/tileview/TileView.html).  Wiki is [here](https://github.com/moagrius/TileView/wiki).

###Installation
Gradle:
```
compile 'com.qozix:tileview:'2.2.6'
```

The library is hosted on jcenter, and is not currently available from maven.

```
repositories {  
   jcenter()  
}
```

###Demo
A demo application, built in Android Studio, is available in the `demo` folder of this repository.  Several use-cases are present; the `RealMapTileViewActivity` is the most substantive.

###Quick Setup
1. Tile an image into image slices of a set size, e.g., 256x256 (<a href="https://github.com/moagrius/TileView/wiki/Creating-Tiles" target="_blank">instructions</a>)
1. Name the tiles by the row and column number, e.g., 'tile-1-2.png' for the image tile that would be
at the 2nd column from left and 3rd row from top.
1. Create a new application with a single activity ('Main').
1. Save the image tiles to your `assets` directory.
1. Add `compile 'com.qozix:tileview:2.2.6'` to your gradle dependencies.
1. In the Main Activity, use this for `onCreate`:
```
@Override
protected void onCreate( Bundle savedInstanceState ) {
  super.onCreate( savedInstanceState );
  TileView tileView = new TileView( this );
  tileView.setSize( 2000, 3000 );  // the original size of the untiled image
  tileView.addDetailLevel( 1f, "tile-%d-%d.png", 256, 256);
  setContentView( tileView );
}
```
That's it.  You should have a tiled image that only renders the pieces of the image that are within the current viewport, and pans and zooms with gestures.

###Basics

####DetailLevels

![detail-levels](https://cloud.githubusercontent.com/assets/701344/10954031/d2059c3e-8310-11e5-821d-26dd8691d4d3.gif)

A TileView instance can have any number of detail levels, which is a single image made up of many tiles; each DetailLevel exists in the same space, but are useful to show different levels of details (thus the class name), and to further break down large images into smaller tiles sets.  These tiles are positioned appropriately to show the portion of the image that the device's viewport is displayed - other tiles are recycled (and their memory freed) as they move out of the visible area.  Detail levels often show the same content at different magnifications, but may show different details as well - for example, a detail level showing a larger area will probably label features differently than a detail level showing a smaller area (imagine a TileView representing the United States may show the Rocky Mountains at a very low detail level, while a higher detail level may show individual streets or addresses.

Each detail level is passed a float value, indicating the scale value that it represents (e.g., a detail level passed 0.5f scale would be displayed when the TileView was zoomed out by 50%). Additionally, each detail level is passed an arbitrary data object that is attached to each tile and can provide instructions on how to generate the tile's bitmap.  That data object is often a String, formatted to provide the path to the bitmap image for that Tile, but can be any kind of Object whatsoever - during the decode process, each tile has access to the data object for the  detail level.

####Tiles

A Tile is a class instance that represents a Bitmap - a portion of the total image.  Each Tile provides position information, and methods to manage the Bitmap's state and behavior.  Each Tile instanced is also passed to the TileView's `BitmapProvider` implementation, which is how individual bitmaps are generated.  Tile instances uses an `equals` method that compares only row, column and detail level, and are often passed in `Set` collections, so that Tile instances already in process are simply excluded by the unique nature of the Set if the program or user tries to add a single Tile more than once.

Each TileView instance must reference a `BitmapProvider` implementation to generate tile bitmaps.  The interface defines a single method: `public Bitmap getBitmap( Tile tile, Context context );`.  This method is called each time a bitmap is required, and has access to the Tile instance for that position and detail level, and a Context object to access system resources.  The `BitmapProvider` implementation can generate the bitmap in any way it chooses - assets, resources, http requests, dynamically drawn, SVG, decoded regions, etc.  The default implementation, `BitmapProviderAssets`, parses a String (the data object passed to the DetailLevel) and returns a bitmap found by file name in the app's assets directory.

####Markers & Callouts

![markers-callouts](https://cloud.githubusercontent.com/assets/701344/10954032/d207ffc4-8310-11e5-926d-038549987d47.gif)

A marker is just a View - any type of View - TextView, ImageView, RelativeLayout, whatever.  A marker does not scale, but it's position updates as the TileView scales, so it's always attached to the original position.  Markers are always laid as as if passed WRAP_CONTENT on both axes.  Markers can have anchor points supplied, which are applied to width and height as offsets - to have a marker center horizontally to a point, and align at the bottom edge (like a typical map pin would do), you'd pass -0.5f and -1.0f (thus, left position is offset by half the width, and top is offset by the full height).

Markers can have traditional touch handlers, like `View.OnClickListener`, but these usually consume the event, so a drag operation might be interrupted when a user's finger crossed a marker View that had a consuming listener.  Instead, consider `TileView.setMarkerTapListener`, which will react when a marker is tapped but will not consume the event.

To use a View as a marker:
```
tileView.addMarker( someView, 250, 500, -0.5f, -1.0f );
```

A callout might be better described as an "info window", and is functionally identical to a marker, with 2 differences: 1, all callouts exist on a layer above markers, and 2, any touch event on the containing TileView instance will remove all callouts.  This would be prevented if the event is consumed (for example, by a `View.OnClickListener` on a button inside the Callout).  Callouts are often opened in response to a marker tap event.

Callouts use roughly the same API as markers.

####HotSpots

A HotSpot represents a region on the TileView that should react when tapped.  The HotSpot class extends `android.graphics.Region` and will virtually scale with the TileView.  In addition to the Region API it inherits, a HotSpot also can accept a "tag" object (any arbitrary data structure), and a `HotSpotTapListener`.  HotSpot taps are not consumed and will not interfere with the touch events examined by the TileView.

To create a HotSpot:
```
HotSpot hotSpot = new HotSpot();
hotSpot.setTag( this );
hotSpot.set( new Rect( 0, 0, 100, 100 ) );  // or any other API to define the region
tileView.addHotSpot( hotSpot, new HotSpot.HotSpotTapListener(){
  @Override
  public void OnHotSpotTap( HotSpot hotSpot, int x, int y ) {
    Activity activity = (Activity) hotSpot.getTag();
    Log.d( "HotSpotTapped", "With access through the tag API to the Activity " + activity );
  }
});
```

####Paths

![paths](https://cloud.githubusercontent.com/assets/701344/10954035/d20aee5a-8310-11e5-9027-ff06bc921a23.gif)

TileView uses `DrawablePath` instances to draw paths above the tile layer.  Paths will transform with the TileView as it scales, but do not deform - that's to say that a 10DP wide stroke will always be 10DP wide, but the points of the path will be scaled with the TileView. 

`DrawablePath` instances are objects that relate an instance of `android.graphics.Path` with an instance of `android.graphics.Paint` - there is no additional direct access API.  Scaling is managed by a singel instance of `CompositePathView`, which also supplies a default `Paint` instance that's used if any individual `DrawablePath` has a `null` value for it `paint` property.

Paths are not Views, and cannot be clicked.  It is possible, however, to use the same `Path` instance on a `HotSpot` and a `DrawablePath`.

*Note that TileView uses `canvas.drawPath` to render paths, which creates a higher-quality graphic, but can be a big hit on performance.*

To add a path:

```
DrawablePath drawablePath = new DrawablePath();
drawablePath.path = // generate a Path using the standard android.graphics.Path API
drawablePath.paint = // generate a Paint instance use the standard android.graphics.Paint API
tileView.addPath( drawablePath );
```

####Scaling
The `setScale(1)` method sets the initial scale of the TileView. 

`setScaleLimits(0, 1)` sets the minimum and maximum scale which controls how far a TileView can be zoomed in or out. `0` means completely zoomed out, `1` means zoomed in to the most detailed level (with the pixels of the tiles matching the screen dpi). For example by using `setScaleLimits(0, 3)` you allow users to zoom in even further then the most detailed level (stretching the image).

`setMinimumScaleMode(ZoomPanLayout.MinimumScaleMode.FILL)` controls how far a image can be zoomed out based on the dimensions of the image:
- `FILL`: Limit the minimum scale to no less than what would be required to fill the container
- `FIT`: Limit the minimum scale to no less than what would be required to fit inside the container
- `NONE`: Limit to the minimum scale level set by `setScaleLimits`

_When using `FILL` or `FIT`, the minimum scale level of `setScaleLimits` is ignored._

####Hooks and Listeners

A TileView can have any number of `ZoomPanListeners` instances listening for events relating to zoom and pan actions, including: `onPanBegin`, `onPanUpdate`, `onPanEnd`, `onZoomBegin`, `onZoomUpdate`, and `onZoomEnd`.  The last argument passed to each callback is the source of the event, represented by `ZoomPanListener.Origin` enum: `DRAG`, `FLING`, `PINCH`, or null (which indicates a programmatic pan or zoom).

To use a ZoomPanListener:

```
tileView.addZoomPanListener( new ZoomPanListener(){
  void onPanBegin( int x, int y, Origination origin ){
    Log.d( "TileView", "pan started..." );
  }
  void onPanUpdate( int x, int y, Origination origin ){}
  void onPanEnd( int x, int y, Origination origin ){}
  void onZoomBegin( float scale, Origination origin ){}
  void onZoomUpdate( float scale, Origination origin ){}
  void onZoomEnd( float scale, Origination origin ){}
});
```

Additionally, TileView reports most significant operations to hooks.  TileView implements `ZoomPanLayout.ZoomPanListener`, `TileCanvasViewGroup.TileRenderListener`, and `DetailLevelManager.DetailLevelChangeListener`, and it's super class implements `GestureDetector.OnGestureListener`, `GestureDetector.OnDoubleTapListener`,`ScaleGestureDetector.OnScaleGestureListener`, and `TouchUpGestureDetector.OnTouchUpListener`.  As such, the following hooks are available to be overridden by subclasses of TileView:

```
protected void onScrollChanged( int l, int t, int oldl, int oldt );
public void onScaleChanged( float scale, float previous );
public void onPanBegin( int x, int y, Origination origin );
public void onPanUpdate( int x, int y, Origination origin );
public void onPanEnd( int x, int y, Origination origin );
public void onZoomBegin( float scale, Origination origin) ;
public void onZoomUpdate( float scale, Origination origin );
public void onZoomEnd( float scale, Origination origin );
public void onDetailLevelChanged( DetailLevel detailLevel );
public boolean onSingleTapConfirmed( MotionEvent event );
public void onRenderStart();
public void onRenderCancelled();
public void onRenderComplete();
public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY );
public boolean onDown( MotionEvent event );
public boolean onFling( MotionEvent event1, MotionEvent event2, float velocityX, float velocityY );
public void onLongPress( MotionEvent event );
public void onShowPress( MotionEvent event );
public boolean onSingleTapUp( MotionEvent event );
public boolean onSingleTapConfirmed( MotionEvent event );
public boolean onDoubleTap( MotionEvent event );
public boolean onDoubleTapEvent( MotionEvent event );
public boolean onScaleBegin( ScaleGestureDetector scaleGestureDetector );
public void onScaleEnd( ScaleGestureDetector scaleGestureDetector );
public boolean onScale( ScaleGestureDetector scaleGestureDetector );
public boolean onTouchUp();
```

Be careful to note where the method was specified, however; for example, `onScaleBegin`, `onScale`, and `onScaleEnd` are provided by `android.view.GestureDetector.OnScaleGestureListener`, so are only aware of scale operations initiated by a gesture (pinch), while `onScaleChanged` is defined by `ZoomPanLayout` and will report any changes to scale from any source, so is probably more useful.  See the [javadocs](http://moagrius.github.io/TileView/index.html?com/qozix/tileview/TileView.html) for specifications.
 
###How Do I...?

####...create tiles from an image?
See the [wiki entry here](https://github.com/moagrius/TileView/wiki/Creating-Tiles).

####...use relative coordinates (like latitude and longitude)?
The TileView method `defineBounds( double left, double top, double right, double bottom )` establishes a coordinate system for further positioning method calls (e.g., `scrollTo`, `addMarker`, etc).  After relative coordinates are established by invoking the `defineBounds` method, any subsequent method invocations that affect position *and* accept `double` parameters will compute the value as relative of the provided bounds, rather than absolute pixels.  That's to say that:
 
 1.  A TileView instance is initialized with `setSize( 5000, 5000 );`
 1.  That TileView instance calls `defineBounds( 0, 100, 0, 100 );`
 1.  That TileView instance calls `scrollTo( 25d, 50d );`
 1.  That TileView will immediately scroll to the pixel at 1250, 2500.
 
This same logic can be used to supply latitude and longitude values to the TileView, by supplying the left and rightmost longitudes, and the top and bottommost latitudes.  Remember that traditional coordinates are expressed (lat, lng), but TileView (and most UI frameworks) expect position values to be expressed as (x, y) - so positioning methods should be sent (lng, lat).

####...use a third party image loading library like Picasso, Glide, UIL, etc?
Implement your own `BitmapProvider`, which has only a single method, then pass an instance of that class to `TileView.setImageProvider`.  Here's an example using Picasso (untested):

```
public class BitmapProviderPicasso implements BitmapProvider {
  public Bitmap getBitmap( Tile tile, Context context ) {
    Object data = tile.getData();
    if( data instanceof String ) {
      String unformattedFileName = (String) tile.getData();
      String formattedFileName = String.format( unformattedFileName, tile.getColumn(), tile.getRow() );
      return Picasso.with( context ).load( path ).get();
    }
    return null;
  }
}
```

And tell the TileView to use it:
```
tileView.setBitmapProvider( new BitmapProviderPicasso() );
```

####...load tile bitmaps from a website?
Again, implement your own `BitmapProvider`.  You could roll your own using `URL` and `BitmapFactory.decodeStream`, or leverage a third-party library intended for downloading images.  Note that the `BitmapProviderPicasso` example above would work with network images out of the box, just make sure the string it's getting is a valid URL:

```
tileView.addDetailLevel( 1.0f, "http://example.com/tiles/%d-%d.png" );
```

####...add my custom View to the TileView, so that it scales?

![scaling-layout](https://cloud.githubusercontent.com/assets/701344/10954036/d235f41a-8310-11e5-810f-d55e58477ec3.gif)

Create a layout, add whatever views you want to it, and pass the layout to `TileView.addScalingViewGroup`:

```
RelativeLayout relativeLayout = new RelativeLayout( this );
ImageView logo = new ImageView( this );
logo.setImageResource( R.drawable.logo );
RelativeLayout.LayoutParams logoLayoutParams = new RelativeLayout.LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT );
logoLayoutParams.addRule( RelativeLayout.CENTER_IN_PARENT );
relativeLayout.addView( logo, logoLayoutParams );
tileView.addScalingViewGroup( relativeLayout );
```

####...add my custom View to the TileView, so that it does *not* scale?

![non-scaling-child](https://cloud.githubusercontent.com/assets/701344/10954034/d20a704c-8310-11e5-8962-20f393ac098a.gif)

TileView is a ViewGroup, and views can be added normally.  No scaling behavior is passed directly, so unless you do something to make it scale, it will behave as would any other View, although the dimensions passed to it will reflect the size defined by the `setSize` API, not the dimensions of the TileView on screen.

Create a layout, add whatever views you want to it, and add it using `addView`:

```
RelativeLayout relativeLayout = new RelativeLayout( this );
ImageView logo = new ImageView( this );
logo.setImageResource( R.drawable.logo );
RelativeLayout.LayoutParams logoLayoutParams = new RelativeLayout.LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT );
logoLayoutParams.addRule( RelativeLayout.CENTER_IN_PARENT );
relativeLayout.addView( logo, logoLayoutParams );
tileView.addView( relativeLayout );
```

####...add a down-sampled image beneath the tile layer?

![downsample](https://cloud.githubusercontent.com/assets/701344/10954030/d20326de-8310-11e5-8d4d-a42b262c2a8c.gif)

Since TileView is a ViewGroup, and it will lay out it's children according to the dimension supplied by the `setSize` API, adding a standard ImageView at index 0 with the image source a small version of the tiled composite image will create the down-sampled effect.  Generally, the image should be low resolution and file size (images smaller than 500 pixels square should be OK).

```
ImageView downSample = new ImageView( this );
downSample.setImageResource( R.drawable.downsampled_image );
tileView.addView( downSample, 0 );
```

###Contributing
See [here](https://github.com/moagrius/TileView/wiki/Contributing).

###Contributors
Several members of the github community have contributed and made `TileView` better, but over the last year or so, @peterLaurence has been as involved as myself and been integral in the last few major updates.  Thanks Peter.
