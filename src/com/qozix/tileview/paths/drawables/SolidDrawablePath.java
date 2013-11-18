package com.qozix.tileview.paths.drawables;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import com.qozix.tileview.paths.DrawablePath;

public class SolidDrawablePath implements DrawablePath {

    private Path path;
    private Paint paint;
    
    @SuppressLint("NewApi")
    @Override
    public void draw(Canvas canvas, Path drawingPath) {
        // quickReject is not supported on hw accelerated canvas versions below 16 but isHardwareAccelerated works only from version 11
        if (android.os.Build.VERSION.SDK_INT >= 11 && canvas.isHardwareAccelerated() && android.os.Build.VERSION.SDK_INT < 16) {
            canvas.drawPath( drawingPath, paint );
        } else if ( !canvas.quickReject( drawingPath, Canvas.EdgeType.BW ) ) {
            canvas.drawPath( drawingPath, paint );
        }
    }
    
    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public void setPath(Path path) {
        this.path = path;
    }

    @Override
    public Paint getPaint() {
        return paint;
    }

    @Override
    public void setPaint(Paint paint) {
        this.paint = paint;
    }

}
