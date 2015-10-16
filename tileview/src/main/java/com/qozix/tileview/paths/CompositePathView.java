package com.qozix.tileview.paths;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class CompositePathView extends View {

  private static final int DEFAULT_STROKE_COLOR = 0xFF000000;
  private static final int DEFAULT_STROKE_WIDTH = 10;

  private float mScale = 1;

  private boolean mShouldDraw = true;

  private Paint mDefaultPaint = new Paint();

  {
    mDefaultPaint.setStyle( Paint.Style.STROKE );
    mDefaultPaint.setColor( DEFAULT_STROKE_COLOR );
    mDefaultPaint.setStrokeWidth( DEFAULT_STROKE_WIDTH );
    mDefaultPaint.setAntiAlias( true );
  }


  private Path mRecyclerPath = new Path();
  private Matrix mMatrix = new Matrix();

  private ArrayList<DrawablePath> mDrawablePaths = new ArrayList<DrawablePath>();

  public CompositePathView( Context context ) {
    super( context );
    setWillNotDraw( false );
  }

  public float getScale() {
    return mScale;
  }

  public void setScale( float scale ) {
    mScale = scale;
    invalidate();
  }

  public Paint getPaint() {
    return mDefaultPaint;
  }

  // TODO: huh?
  public Path getPathFromPoints( List<Point> points ) {
    Path path = new Path();
    Point start = points.get( 0 );
    path.moveTo( (float) start.x, (float) start.y );
    int l = points.size();
    for( int i = 1; i < l; i++ ) {
      Point point = points.get( i );
      path.lineTo( (float) point.x, (float) point.y );
    }
    return path;
  }

  public DrawablePath addPath( List<Point> points ) {
    return addPath( points, mDefaultPaint );
  }

  public DrawablePath addPath( List<Point> points, Paint paint ) {
    Path path = new Path();
    Point start = points.get( 0 );
    path.moveTo( (float) start.x, (float) start.y );
    int l = points.size();
    for( int i = 1; i < l; i++ ) {
      Point point = points.get( i );
      path.lineTo( (float) point.x, (float) point.y );
    }
    return addPath( path, paint );
  }

  public DrawablePath addPath( Path path, Paint paint ) {
    DrawablePath DrawablePath = new DrawablePath();
    DrawablePath.path = path;
    DrawablePath.paint = paint;
    return addPath( DrawablePath );
  }

  public DrawablePath addPath( Path path ) {
    return addPath( path, mDefaultPaint );
  }

  public DrawablePath addPath( DrawablePath DrawablePath ) {
    mDrawablePaths.add( DrawablePath );
    invalidate();
    return DrawablePath;
  }

  public void removePath( DrawablePath path ) {
    mDrawablePaths.remove( path );
    invalidate();
  }

  public void clear() {
    mDrawablePaths.clear();
    invalidate();
  }

  public void setShouldDraw( boolean should ) {
    mShouldDraw = should;
    invalidate();
  }

  @Override
  public void onDraw( Canvas canvas ) {
    if( mShouldDraw ) {
      mMatrix.setScale( mScale, mScale );
      for( DrawablePath DrawablePath : mDrawablePaths ) {
        mRecyclerPath.set( DrawablePath.path );
        mRecyclerPath.transform( mMatrix );
        canvas.drawPath( mRecyclerPath, DrawablePath.paint );
      }
    }
    super.onDraw( canvas );
  }

}
