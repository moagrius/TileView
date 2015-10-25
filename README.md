#Version 2.0

**Version 2.0 released 10.25.15**

Version 2 is a major version change and is not backwards compatible with 1.x versions.  The API has
changed but will still be familiar to users of 1.x.

Note that the original version of this library was written in early 2011.  Version 2 is the first
major upgrade, and aims to provide a simpler API to a more robust and performant codebase.

Major goals were:

1.  Optimize tiling.  Tiles are now simply POJOs that manage Bitmaps, and are no longer ImageView instances.
2.  Leverage the gestures framework.  In order to behave more consistently with other Android widgets, several (all?) framework-provided gesture detector classes are used in version 2.
3.  Leverage the threading framework.  Threading is managed by AsyncTasks leveraging a common ThreadPoolExecutor.
4.  Simplify and expose.  The API provides fewer overloaded signatures, but public access to nearly all core classes.
5.  Defer caching to the user.  Built-in caching has been removed.  The user can supply their own (or a third party) caching mechanism using the BitmapProvider interface.
6.  General refactoring.  There are too many simplifications and optimization to mention, but each class and each method has been revisited.
7.  Hooks hooks hooks!  While pan and zoom events are broadcast using a simple, familiar listener mechanism, and should be sufficient for most use-cases, public hooks exist for a large number of operations that can be overriden by subclasses for custom functionality.

<!--
#Version 2 Goals
X1.  Eliminate strings from detail levels
X1.  Eliminate ImageViews as Tiles
X1.  Single package
X1.  Eliminate hacked core classes
X1.  Better thread management
1.  Issues
    Xa. Disable zoom on double tap feature request - extend and override onDoubleTap (no super)
    Xb. Smooth scale to position feature request
    -c. Skipped detail level feature request
    Xd. moveMarker(x, y) overrides anchor feature request
    Xe. setScaleLimit does not work with setScaleToFit(false) bug
X1.  Allow layer insertion and scaling layer insertion
X1.  Eliminate downsample paradigm
X1.  Include a sample downsample
X1.  Remove LRU cache dependency
X1.  Use more framework-provided functionality, like GestureListener
X1.  End fling (test for change)
X1.  Make most private into protected for extensibility
X1.  Other Issues
    Xa.  when detail levels change with no downsample, there's a moment when the old one dies before the new one is done.
X1.  Don't use detail manager as intermediary anymore
X1.  BitmapDecoder to TileProvider or Adapter paradigm
X1.  Remove TileSetSelector
-1.  consider generics in the arbitrary data Object for detail levels
X1.  optimize data structures
X1.  optimize tile set comparisons
-1.  set downsample (addView imageView)?
-->











<!--
  // android.view.View
  protected void onScrollChanged( int l, int t, int oldl, int oldt );
  // com.qozix.tileview.widgets.ZoomPanLayout
  public void onScaleChanged( float scale, float previous );
  // com.qozix.tileview.widgets.ZoomPanLayout.ZoomPanListener
  public void onPanBegin( int x, int y, Origination origin );
  public void onPanUpdate( int x, int y, Origination origin );
  public void onPanEnd( int x, int y, Origination origin );
  public void onZoomBegin( float scale, float focusX, float focusY, Origination origin );
  public void onZoomUpdate( float scale, float focusX, float focusY, Origination origin );
  public void onZoomEnd( float scale, float focusX, float focusY, Origination origin );
  // com.qozix.tileview.details.DetailLevel.DetailLevelChangeListener
  public void onDetailLevelChanged( DetailLevel detailLevel );
  // android.view.GestureDetector.OnDoubleTapListener
  public boolean onSingleTapConfirmed( MotionEvent event );
  // com.qozix.TileRenderTask.TileRenderListener
  public void onRenderStart();
  public void onRenderCancelled();
  public void onRenderComplete();
  // android.view.GestureDetector.OnGestureListener
  public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY );
  public boolean onDown( MotionEvent event );
  public boolean onFling( MotionEvent event1, MotionEvent event2, float velocityX, float velocityY );
  public void onLongPress( MotionEvent event );
  public void onShowPress( MotionEvent event );
  public boolean onSingleTapUp( MotionEvent event );
  // android.view.GestureDetector.OnDoubleTapListener
  public boolean onSingleTapConfirmed( MotionEvent event );
  public boolean onDoubleTap( MotionEvent event );
  public boolean onDoubleTapEvent( MotionEvent event );
  // android.view.GestureDetector.OnScaleGestureListener
  public boolean onScaleBegin( ScaleGestureDetector scaleGestureDetector );
  public void onScaleEnd( ScaleGestureDetector scaleGestureDetector );
  public boolean onScale( ScaleGestureDetector scaleGestureDetector );
  // com.qozix.tileview.view.TouchUpGestureDetectorOnTouchUpListener
  public boolean onTouchUp();
  // android.animation.ValueAnimator.AnimatorUpdateListener
  public void onAnimationUpdate( ValueAnimator valueAnimator );
  // android.animation.ValueAnimator.AnimatorListener
  public void onAnimationStart( Animator animator );
  public void onAnimationEnd( Animator animator );
  public void onAnimationCancel( Animator animator );
  public void onAnimationRepeat( Animator animator );
-->

#TileView
The TileView widget is a subclass of ViewGroup that provides a mechanism to asynchronously display tile-based images,
with additional functionality for 2D dragging, flinging, pinch or double-tap to zoom, adding overlaying Views (markers),
built-in Hot Spot support, dynamic path drawing, multiple levels of detail, and support for any relative positioning or
coordinate system.

![Demo Gif](https://cloud.githubusercontent.com/assets/701344/10254372/da002dea-6908-11e5-920c-814b09c90c80.gif)

###Documentation
Javadocs are [here](http://moagrius.github.io/TileView/index.html?com/qozix/tileview/TileView.html).
Wiki is [here](https://github.com/moagrius/TileView/wiki).

###Installation
Gradle:
```
compile 'com.qozix:tileview:2.0.0'
```

###Demo
A demo application, built in Android Studio, is available in the `demo` folder of this repository.
Several use-cases are present; the `RealMapTileViewActivity` is the most substantive.

###Quick Setup
1. Tile an image into image slices of a set size, e.g., 256x256 (<a href="https://github.com/moagrius/TileView/wiki/Creating-Tiles" target="_blank">instructions</a>)
1. Name the tiles by the row and column number, e.g., 'tile-2-3.png' for the image tile that would be
at the 2nd column from left and 3rd row from top.
1. Create a new application with a single activity ('Main').
1. Save the image tiles to your `assets` directory.
1. Add `compile 'com.qozix:tileview:2.0.0'` to your gradle dependencies.
1. In the Main Activity, use this for `onCreate`:
```
@Override
protected void onCreate( Bundle savedInstanceState ) {
  super.onCreate( savedInstanceState );
  TileView tileView = new TileView( this );
  tileView.setSize( 2000, 3000 );  // the original size of the untiled image
  tileView.addDetailLevel( 1f, "tile-%col%-%row%.png");
  setContentView( tileView );
}
```
That's it.  You should have a tiled image that only renders the pieces of the image that are
within the current viewport, and pans and zooms with gestures.

