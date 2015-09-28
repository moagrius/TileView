<h1>TileView</h1>

<h2 style="color:red;">Update: this repo has been updated to include a demo app,
and the widget code as an Android Studio library module.
All other `TileView` related repos will be deprecated.</h2>

<p>The TileView widget is a subclass of ViewGroup that provides a mechanism to asynchronously display tile-based images,
with additional functionality for 2D dragging, flinging, pinch or double-tap to zoom, adding overlaying Views (markers),
built-in Hot Spot support, dynamic path drawing, multiple levels of detail, and support for any relative positioning or
coordinate system.</p>

<a target="_blank" href="http://www.youtube.com/watch?v=N9fzrZDqAZY">
  <img src="http://img.youtube.com/vi/N9fzrZDqAZY/1.jpg" />
</a><a target="_blank" href="http://www.youtube.com/watch?v=N9fzrZDqAZY">
  <img src="http://img.youtube.com/vi/N9fzrZDqAZY/2.jpg" />
</a><a target="_blank" href="http://www.youtube.com/watch?v=N9fzrZDqAZY">
  <img src="http://img.youtube.com/vi/N9fzrZDqAZY/3.jpg" />
</a>

<h4>Documentation</h4>
<p>Javadocs are <a href="http://moagrius.github.io/TileView/index.html?com/qozix/tileview/TileView.html" target="_blank">here</a>.
Wiki is <a href="https://github.com/moagrius/TileView/wiki">here</a>.</p>

<h4>Installation</h4>
Gradle:
```
compile 'com.qozix:tileview:1.0.15'
```

<h4>Demo</h4>
<p>A demo application, built in Android Studio, is available in the `demo` folder of this repository.
Several use-cases are present; the `RealMapTileViewActivity` is the most substantive.

<h4>Quick Setup</h4>
1. Tile an image into image slices of a set size, e.g., 256x256 (<a href="https://github.com/moagrius/TileView/wiki/Creating-Tiles" target="_blank">instructions</a>)
1. Name the tiles by the row and column number, e.g., 'tile-2-3.png' for the image tile that would be
at the 2nd column from left and 3rd row from top.
1. Create a new application with a single activity ('Main').
1. Save the image tiles to your `assets` directory.
1. Add `compile 'com.qozix:tileview:1.0.15'` to your gradle dependencies.
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