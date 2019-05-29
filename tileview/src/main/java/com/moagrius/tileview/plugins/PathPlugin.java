package com.moagrius.tileview.plugins;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.view.View;

import com.moagrius.tileview.TileView;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PathPlugin implements TileView.Plugin, TileView.CanvasDecorator {

  private static final int DEFAULT_STROKE_COLOR = 0xFF000000;
  private static final int DEFAULT_STROKE_WIDTH = 10;

  private View mInvalidater;
  private Path mRecyclerPath = new Path();
  private Paint mDefaultPaint = new Paint();
  private Set<DrawablePath> mDrawablePaths = new LinkedHashSet<>();

  {
    mDefaultPaint.setStyle(Paint.Style.STROKE);
    mDefaultPaint.setColor(DEFAULT_STROKE_COLOR);
    mDefaultPaint.setStrokeWidth(DEFAULT_STROKE_WIDTH);
    mDefaultPaint.setAntiAlias(true);
  }

  @Override
  public void install(TileView tileView) {
    tileView.addCanvasDecorator(this);
    mInvalidater = tileView;
  }

  @Override
  public void decorate(Canvas canvas) {
    for (DrawablePath drawablePath : mDrawablePaths) {
      mRecyclerPath.set(drawablePath.getPath());
      canvas.drawPath(mRecyclerPath, drawablePath.getPaint());
    }
  }

  public DrawablePath drawPath(List<Point> positions, Paint paint) {
    Path path = new Path();
    Point start = positions.get(0);
    path.moveTo(start.x, start.y);
    for (int i = 1; i < positions.size(); i++) {
      Point position = positions.get(i);
      path.lineTo(position.x, position.y);
    }
    DrawablePath drawablePath = addPath(path, paint);
    mInvalidater.invalidate();
    return drawablePath;
  }

  public DrawablePath addPath(Path path, Paint paint) {
    if (paint == null) {
      paint = mDefaultPaint;
    }
    return addPath(new DrawablePath(path, paint));
  }

  public DrawablePath addPath(DrawablePath drawablePath) {
    mDrawablePaths.add(drawablePath);
    return drawablePath;
  }

  public void removePath(DrawablePath path) {
    mDrawablePaths.remove(path);
  }

  public void clear() {
    mDrawablePaths.clear();
  }

  public static class DrawablePath {
    private Path mPath;
    private Paint mPaint;

    public DrawablePath(Path path, Paint paint) {
      mPath = path;
      mPaint = paint;
    }

    public Path getPath() {
      return mPath;
    }

    public Paint getPaint() {
      return mPaint;
    }
  }

}
