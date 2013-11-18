package com.qozix.tileview.paths;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public interface DrawablePath {

    /**
     * Returns the path that this drawable will follow.
     * 
     * @return
     */
    public Path getPath();

    /**
     * Set path for this drawable.
     * 
     * @param path
     */
    public void setPath(final Path path);

    /**
     * Returns the paint to be used for this path.
     * 
     * @return
     */
    public Paint getPaint();

    /**
     * Set the paint for this path.
     * 
     * @param paint
     */
    public void setPaint(final Paint paint);

    /**
     * Draw the supplied path onto the supplied canvas.
     * 
     * @param canvas
     * @param drawingPath
     */
    public void draw(final Canvas canvas, final Path drawingPath);

}
