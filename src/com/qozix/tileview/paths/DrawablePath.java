package com.qozix.tileview.paths;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public class DrawablePath {

    private Path path;
    private Paint paint;

    /**
     * Returns the path that this drawable will follow.
     * 
     * @return
     */

    public Path getPath() {
        return path;
    }

    /**
     * Set path for this drawable.
     * 
     * @param path
     */

    public void setPath(Path path) {
        this.path = path;
    }

    /**
     * Returns the paint to be used for this path.
     * 
     * @return
     */

    public Paint getPaint() {
        return paint;
    }

    /**
     * Set the paint for this path.
     * 
     * @param paint
     */

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    /**
     * Draw the supplied path onto the supplied canvas.
     * 
     * @param canvas
     * @param drawingPath
     */
    @SuppressLint("NewApi")
    public void draw(Canvas canvas, Path drawingPath) {
        // quickReject is not supported on hw accelerated canvas versions below
        // 16 but isHardwareAccelerated works only from version 11
        if (android.os.Build.VERSION.SDK_INT >= 11 && canvas.isHardwareAccelerated()
                && android.os.Build.VERSION.SDK_INT < 16) {
            canvas.drawPath(drawingPath, paint);
        }
        else if (!canvas.quickReject(drawingPath, Canvas.EdgeType.BW)) {
            canvas.drawPath(drawingPath, paint);
        }
    }

}
