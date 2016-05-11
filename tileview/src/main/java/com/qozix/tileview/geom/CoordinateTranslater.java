package com.qozix.tileview.geom;

import android.graphics.Path;

import java.util.List;

/**
 * Helper class to translate relative coordinates into absolute pixels.
 * Note that these methods always take arguments x and y in that order;
 * this may be counter-intuitive since coordinates are often expressed as lat (y), lng (x).
 * When using translation methods of this class, pass latitude and longitude in the reverse
 * order: translationMethod( longitude, latitude )
 */
public class CoordinateTranslater {

  private double mLeft;
  private double mTop;
  private double mRight;
  private double mBottom;

  private double mDiffX;
  private double mDiffY;

  private int mWidth;
  private int mHeight;

  private boolean mHasDefinedBounds;

  /**
   * Set size in pixels of the image at 100% scale.
   *
   * @param width  Width of the tiled image in pixels.
   * @param height Height of the tiled image in pixels.
   */
  public void setSize( int width, int height ) {
    mWidth = width;
    mHeight = height;
  }

  /**
   * Define arbitrary bound coordinates to the edges of the tiled image (e.g., latitude and longitude).
   *
   * @param left   The left boundary (e.g., west longitude).
   * @param top    The top boundary (e.g., north latitude).
   * @param right  The right boundary (e.g., east longitude).
   * @param bottom The bottom boundary (e.g., south latitude).
   */
  public void setBounds( double left, double top, double right, double bottom ) {
    mHasDefinedBounds = true;
    mLeft = left;
    mTop = top;
    mRight = right;
    mBottom = bottom;
    mDiffX = mRight - mLeft;
    mDiffY = mBottom - mTop;
  }

  public void unsetBounds() {
    mHasDefinedBounds = false;
    mLeft = 0;
    mTop = 0;
    mRight = mWidth;
    mBottom = mHeight;
    mDiffX = mWidth;
    mDiffY = mHeight;
  }

  /**
   * Translate a relative X position to an absolute pixel value.
   *
   * @param x The relative X position (e.g., longitude) to translate to absolute pixels.
   * @return The translated position as a pixel value.
   */
  public int translateX( double x ) {
    if( !mHasDefinedBounds ) {
      return (int) x;
    }
    double factor = (x - mLeft) / mDiffX;
    return FloatMathHelper.scale( mWidth, (float) factor );
  }

  /**
   * Translate a relative X position to an absolute pixel value, considering a scale value as well.
   *
   * @param x The relative X position (e.g., longitude) to translate to absolute pixels.
   * @return The translated position as a pixel value.
   */
  public int translateAndScaleX( double x, float scale ) {
    return FloatMathHelper.scale( translateX( x ), scale );
  }

  /**
   * Translate a relative Y position to an absolute pixel value.
   *
   * @param y The relative Y position (e.g., latitude) to translate to absolute pixels.
   * @return The translated position as a pixel value.
   */
  public int translateY( double y ) {
    if( !mHasDefinedBounds ) {
      return (int) y;
    }
    double factor = (y - mTop) / mDiffY;
    return FloatMathHelper.scale( mHeight, (float) factor );
  }

  /**
   * Translate a relative Y position to an absolute pixel value, considering a scale value as well.
   *
   * @param y The relative Y position (e.g., latitude) to translate to absolute pixels.
   * @return The translated position as a pixel value.
   */
  public int translateAndScaleY( double y, float scale ) {
    return FloatMathHelper.scale( translateY( y ), scale );
  }

  /**
   * Translate an absolute pixel value to a relative coordinate.
   *
   * @param x The x value to be translated.
   * @return The relative value of the x coordinate supplied.
   */
  public double translateAbsoluteToRelativeX( float x ) {
    return mLeft + ( x * mDiffX / mWidth );
  }

  /**
   * Pipes to {@link #translateAbsoluteToRelativeX( float )}
   */
  public double translateAbsoluteToRelativeX( int x ) {
    return translateAbsoluteToRelativeX( (float) x );
  }

  /**
   * Convenience method to translate an absolute pixel value to a relative coordinate, while considering a scale value.
   *
   * @param x     The x value to be translated.
   * @param scale The scale to apply.
   * @return The relative value of the x coordinate supplied.
   */
  public double translateAndScaleAbsoluteToRelativeX( float x, float scale ) {
    return translateAbsoluteToRelativeX( x / scale );
  }

  /**
   * @see #translateAndScaleAbsoluteToRelativeX(float, float)
   */
  public double translateAndScaleAbsoluteToRelativeX( int x, float scale ) {
    return translateAbsoluteToRelativeX( x / scale );
  }

  /**
   * Translate an absolute pixel value to a relative coordinate.
   *
   * @param y The y value to be translated.
   * @return The relative value of the y coordinate supplied.
   */
  public double translateAbsoluteToRelativeY( float y ) {
    return mTop + ( y * mDiffY / mHeight );
  }

  /**
   * Pipes to {@link #translateAbsoluteToRelativeY( float )}
   */
  public double translateAbsoluteToRelativeY( int y ) {
    return translateAbsoluteToRelativeY( (float) y );
  }

  /**
   * Convenience method to translate an absolute pixel value to a relative coordinate, while considering a scale value.
   *
   * @param y     The y value to be translated.
   * @param scale The scale to apply.
   * @return The relative value of the y coordinate supplied.
   */
  public double translateAndScaleAbsoluteToRelativeY( float y, float scale ) {
    return translateAbsoluteToRelativeY( y / scale );
  }

  /**
   * @see #translateAndScaleAbsoluteToRelativeY(float, float)
   */
  public double translateAndScaleAbsoluteToRelativeY( int y, float scale ) {
    return translateAbsoluteToRelativeY( y / scale );
  }

  /**
   * Determines if a given position (x, y) falls within the bounds defined, or the absolute pixel size of the image, if arbitrary bounds are nor supplied.
   *
   * @param x The x value of the coordinate to test.
   * @param y The y value of the coordinate to test.
   * @return True if the point falls within the defined area; false if not.
   */
  public boolean contains( double x, double y ) {
    double top = mTop;
    double bottom = mBottom;
    double left = mLeft;
    double right = mRight;
    if( mTop > mBottom ) {
      top = mBottom;
      bottom = mTop;
    }
    if( mLeft > mRight ) {
      left = mRight;
      right = mLeft;
    }
    return y >= top
      && y <= bottom
      && x >= left
      && x <= right;
  }

  /**
   * Convenience method to convert a List of coordinates (pairs of doubles) to a Path instance.
   *
   * @param positions   List of coordinates (pairs of doubles).
   * @param shouldClose True if the path should be closed at the end of this operation.
   * @return The Path instance created from the positions supplied.
   */
  public Path pathFromPositions( List<double[]> positions, boolean shouldClose ) {
    Path path = new Path();
    double[] start = positions.get( 0 );
    path.moveTo( translateX( start[0] ), translateY( start[1] ) );
    for( int i = 1; i < positions.size(); i++ ) {
      double[] position = positions.get( i );
      path.lineTo( translateX( position[0] ), translateY( position[1] ) );
    }
    if( shouldClose ) {
      path.close();
    }
    return path;
  }

}