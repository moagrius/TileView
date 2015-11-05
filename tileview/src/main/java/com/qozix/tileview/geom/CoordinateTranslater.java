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


  public void setSize( int width, int height ) {
    mWidth = width;
    mHeight = height;
  }

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

  public int translateX( double x ) {
    if( !mHasDefinedBounds ) {
      return (int) x;
    }
    double factor = (x - mLeft) / mDiffX;
    return FloatMathHelper.scale( mWidth, (float) factor );
  }

  public int translateAndScaleX( double x, float scale ) {
    return FloatMathHelper.scale( translateX( x ), scale );
  }

  public int translateY( double y ) {
    if( !mHasDefinedBounds ) {
      return (int) y;
    }
    double factor = (y - mTop) / mDiffY;
    return FloatMathHelper.scale( mHeight, (float) factor );
  }

  public int translateAndScaleY( double y, float scale ) {
    return FloatMathHelper.scale( translateY( y ), scale );
  }

  public boolean contains( double x, double y ) {
    return y >= mTop
      && y <= mBottom
      && x >= mLeft
      && x <= mRight;
  }

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