package com.qozix.tileview.paths;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public class DrawablePath {

	/**
	 * The path that this drawable will follow.
	 */
	public Path path;

	/**
	 * The paint to be used for this path.
	 */
	public Paint paint;

	/**
	 * Draw the supplied path onto the supplied canvas.
	 * 
	 * @param canvas
	 * @param drawingPath
	 */
	@SuppressLint("NewApi")
	public void draw( Canvas canvas, Path drawingPath ) {
		// quickReject is not supported on hw accelerated canvas versions below 16 but isHardwareAccelerated works only from version 11
		if ( android.os.Build.VERSION.SDK_INT >= 11 && canvas.isHardwareAccelerated() && android.os.Build.VERSION.SDK_INT < 16 ) {
			canvas.drawPath( drawingPath, paint );
		} else if ( !canvas.quickReject( drawingPath, Canvas.EdgeType.BW ) ) {
			canvas.drawPath( drawingPath, paint );
		}
	}

}
