package com.qozix.tileview.geom;

import android.graphics.Path;

import java.util.List;

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
    return (int) (0.5 + (mWidth * factor));
  }

  public int translateAndScaleX( double x, float scale ) {
    return (int) (0.5 + translateX( x ) * scale);
  }

  public int translateY( double y ) {
    if( !mHasDefinedBounds ) {
      return (int) y;
    }
    double factor = (y - mTop) / mDiffY;
    return (int) (0.5 + (mHeight * factor));
  }

  public int translateAndScaleY( double y, float scale ) {
    return (int) (0.5 + translateY( y ) * scale);
  }

  public boolean contains( double x, double y ) {
    return y >= mTop
      && y <= mBottom
      && x >= mLeft
      && x <= mRight;
  }

  public Path pathFromPositions( List<double[]> positions ) {
    Path path = new Path();
    double[] start = positions.remove( 0 );
    path.moveTo( translateX( start[0] ), translateY( start[1] ) );
    for( double[] position : positions ) {
      path.lineTo( translateX( position[0] ), translateY( position[1] ) );
    }
    path.close();
    return path;
  }

}