<h1>TileView</h1>
<p>The TileView widget is a subclass of ViewGroup that provides a mechanism to asynchronously display tile-based images,
with additional functionality for 2D dragging, flinging, pinch or double-tap to zoom, adding overlaying Views (markers),
built-in Hot Spot support, dynamic path drawing, multiple levels of detail, and support for any relative positioning or 
coordinate system.</p>

<a target="_blank" href="http://www.youtube.com/watch?v=N9fzrZDqAZY">
  <img src="http://img.youtube.com/vi/N9fzrZDqAZY/1.jpg" />
</a>

<a target="_blank" href="http://www.youtube.com/watch?v=N9fzrZDqAZY">
  <img src="http://img.youtube.com/vi/N9fzrZDqAZY/2.jpg" />
</a>

<a target="_blank" href="http://www.youtube.com/watch?v=N9fzrZDqAZY">
  <img src="http://img.youtube.com/vi/N9fzrZDqAZY/3.jpg" />
</a>

<h4>Documentation</h4>
<p>Javadocs are <a href="http://moagrius.github.io/TileView/index.html?com/qozix/tileview/TileView.html" target="_blank">here</a>.
Wiki is <a href="https://github.com/moagrius/TileView/wiki">here</a>.</p>

<h4>Installation</h4>
<p>
  The widget is straight java, so you can just use the .java files found here (with the dependencies mentioned below).
  A github release (jar, or zip) is available <a target="_blank" href="https://github.com/moagrius/TileView/releases">here</a>.
  Recent versions of Eclipse with ADT can create projects with the compatability library already installed, and will automatically
  add jars in /libs/ to the build path, so just create a new project, add the tileviewlib.jar and the lrucache jar to /libs/ and
  you can start using the class.
</p>

<h4>Demo</h4>
<p>A demo application, built in Eclipse, is available <a target="_blank" href="https://github.com/moagrius/TileViewDemo">here</a>.
<p>The .apk can be <a href="http://moagrius.github.io/TileView/TileViewDemo.apk">downloaded directly</a>.

<h4>Implementation</h4>
<p>A minimal implementation might look like this:</p>
<pre>TileView tileView = new TileView(this);
tileView.setSize(3000,5000);
tileView.addDetailLevel(1.0f, "path/to/tiles/%col%-%row%.jpg");</pre>

<h4>Dependencies</h4>
<p>
  If you're targetting APIs less than 12, you'll need the 
  <a target="_blank" href="http://developer.android.com/tools/extras/support-library.html">Android compatability lib</a>
  for the LruCache implementation.
</p>
<p>
  <a target="_blank" href="https://github.com/JakeWharton/DiskLruCache">Jake Wharton's DiskLruCache</a> is also used.
  <a target="_blank" href="https://oss.sonatype.org/content/repositories/releases/com/jakewharton/disklrucache/1.3.1/disklrucache-1.3.1.jar">Here's</a> a direct link to that jar.
</p>

<h4>Maven users</h4>
```xml
<dependency>
	<groupId>com.qozix</groupId>
	<artifactId>TileView</artifactId>
	<version>1.0.13</version>
</dependency>
```

<h4>Gradle users</h4>
```xml
   compile 'com.qozix:TileView:1.0.13'
```

<h4>Releases</h4>
<p>Check <a target="_blank" href="https://github.com/moagrius/TileView/releases">here</a> for the the latest release.</p>
