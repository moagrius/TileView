<h1>TileView</h1>
<p>The TileView widget is a subclass of ViewGroup that provides a mechanism to asynchronously display tile-based images,
with additional functionality for 2D dragging, flinging, pinch or double-tap to zoom, adding overlaying Views (markers),
built-in Hot Spot support, dynamic path drawing, multiple levels of detail, and support for any relative positioning or 
coordinate system.</p>
 
<h4>Installation</h4>
<p>
  The widget is straight java, so you can just use the .java files found here (with the dependencies mentioned below).
  A jar will be available soon.
</p>

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
	<groupId>com.github.moagrius</groupId>
	<artifactId>TileView</artifactId>
	<version>1.0.0</version>
</dependency>
```

<h4>Documentation</h4>
<p>Javadocs are <a href="http://moagrius.github.io/TileView/index.html?com/qozix/tileview/TileView.html" target="_blank">here</a>.
Wiki is <a href="https://github.com/moagrius/TileView/wiki">here</a>.</p>

<h4>License</h4>
<p>Copyright 2013 Mike Dunn</p>

<p>Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:</p>

<p>The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.</p>

<p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.</p>
