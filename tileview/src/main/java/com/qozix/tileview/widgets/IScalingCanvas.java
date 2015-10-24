package com.qozix.tileview.widgets;

/**
 * @author Mike Dunn, 10/24/15.
 *
 * This interface is simply a flag for Views that will scale their own canvas.  Since it's not
 * possible (AFAIK) to increase the size of the canvas drawn to in a View's onDraw method,
 * we must pre-supply reverse scaled dimensions.
 */
public interface IScalingCanvas {
}
