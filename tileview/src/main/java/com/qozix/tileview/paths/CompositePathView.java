package com.qozix.tileview.paths;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.HashSet;

public class CompositePathView extends View {

  private static final int DEFAULT_STROKE_COLOR = 0xFF000000;
  private static final int DEFAULT_STROKE_WIDTH = 10;

  private float mScale = 1;

  private boolean mShouldDraw = true;

  private Path mRecyclerPath = new Path();
  private Matrix mMatrix = new Matrix();

  private HashSet<DrawablePath> mDrawablePaths = new HashSet<DrawablePath>();

  private Paint mDefaultPaint = new Paint();

  {
    mDefaultPaint.setStyle( Paint.Style.STROKE );
    mDefaultPaint.setColor( DEFAULT_STROKE_COLOR );
    mDefaultPaint.setStrokeWidth( DEFAULT_STROKE_WIDTH );
    mDefaultPaint.setAntiAlias( true );
  }

  public CompositePathView( Context context ) {
    super( context );
    setWillNotDraw( false );
  }

  public float getScale() {
    return mScale;
  }

  public void setScale( float scale ) {
    mScale = scale;
    mMatrix.setScale( mScale, mScale );
    invalidate();
  }

  public Paint getDefaultPaint() {
    return mDefaultPaint;
  }

  public DrawablePath addPath( Path path, Paint paint ) {
    if( paint == null ) {
      paint = mDefaultPaint;
    }
    DrawablePath DrawablePath = new DrawablePath();
    DrawablePath.path = path;
    DrawablePath.paint = paint;
    return addPath( DrawablePath );
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

  public void setShouldDraw( boolean shouldDraw ) {
    mShouldDraw = shouldDraw;
    invalidate();
  }

  @Override
  public void onDraw( Canvas canvas ) {
    if( mShouldDraw ) {
      for( DrawablePath drawablePath : mDrawablePaths ) {
        mRecyclerPath.set( drawablePath.path );
        mRecyclerPath.transform( mMatrix );
        canvas.drawPath( mRecyclerPath, drawablePath.paint );
      }
    }
    super.onDraw( canvas );
  }

  public static class DrawablePath {

    /**
     * The path that this drawable will follow.
     */
    public Path path;

    /**
     * The paint to be used for this path.
     */
    public Paint paint;

  }
}
