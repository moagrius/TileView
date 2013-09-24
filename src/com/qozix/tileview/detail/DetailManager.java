package com.qozix.tileview.detail;

import java.util.HashSet;

import android.graphics.Rect;

public class DetailManager
{
    // selection methods for the different tile sets
    public static final int                         METHOD_DEFAULT            = 0;
    public static final int                         METHOD_CLOSEST            = 1;
    public static final int                         METHOD_RANGES             = 2;

    private static final double                     PRECISION                 = 6;
    private static final double                     DECIMAL                   = Math.pow(10, DetailManager.PRECISION);

    private final DetailLevelSet                    detailLevels              = new DetailLevelSet();
    private final HashSet<DetailLevelEventListener> detailLevelEventListeners = new HashSet<DetailLevelEventListener>();
    private final HashSet<DetailLevelSetupListener> detailLevelSetupListeners = new HashSet<DetailLevelSetupListener>();

    private double                                  scale                     = 1;
    private double                                  historicalScale;

    private DetailLevel                             currentDetailLevel;

    private int                                     width;
    private int                                     height;
    private int                                     scaledWidth;
    private int                                     scaledHeight;

    private boolean                                 detailLevelLocked         = false;

    private int                                     padding                   = 0;
    private final Rect                              viewport                  = new Rect();
    private final Rect                              computedViewport          = new Rect();

    private DetailLevelPatternParser                detailLevelPatternParser  = new DetailLevelPatternParserDefault();

    // the method used to select between the different tile sets
    private int                                     selectionMethod           = DetailManager.METHOD_DEFAULT;

    private static double getAtPrecision(final double s)
    {
        return Math.round(s * DetailManager.DECIMAL) / DetailManager.DECIMAL;
    }

    public DetailManager()
    {
        this.update(true);
    }

    public double getScale()
    {
        return this.scale;
    }

    public void setScale(double s)
    {
        // round to PRECISION decimal places
        // DEBUG: why are we rounding still?
        s = DetailManager.getAtPrecision(s);
        // is it changed?
        final boolean changed = (this.scale != s);
        // set it
        this.scale = s;
        // update computed values
        this.update(changed);
    }

    public int getWidth()
    {
        return this.width;
    }

    public int getHeight()
    {
        return this.height;
    }

    // DEBUG: needed?  maybe use ZPL's width and height...?
    public int getScaledWidth()
    {
        return this.scaledWidth;
    }

    public int getScaledHeight()
    {
        return this.scaledHeight;
    }

    public void setSize(final int w, final int h)
    {
        this.width = w;
        this.height = h;
        this.update(true);
    }

    /**
     *  "pads" the viewport by the number of pixels passed.  e.g., setPadding( 100 ) instructs the
     *  DetailManager to interpret it's actual viewport offset by 100 pixels in each direction (top, left,
     *  right, bottom), so more tiles will qualify for "visible" status when intersections are calculated.
     * @param pixels (int) the number of pixels to pad the viewport by
     */
    public void setPadding(final int pixels)
    {
        this.padding = pixels;
        this.updateComputedViewport();
    }

    public void updateViewport(final int left, final int top, final int right, final int bottom)
    {
        this.viewport.set(left, top, right, bottom);
        this.updateComputedViewport();
    }

    private void updateComputedViewport()
    {
        this.computedViewport.set(this.viewport);
        this.computedViewport.top -= this.padding;
        this.computedViewport.left -= this.padding;
        this.computedViewport.bottom += this.padding;
        this.computedViewport.right += this.padding;
    }

    public Rect getViewport()
    {
        return this.viewport;
    }

    public Rect getComputedViewport()
    {
        return this.computedViewport;
    }

    public DetailLevelPatternParser getDetailLevelPatternParser()
    {
        return this.detailLevelPatternParser;
    }

    public void setDetailLevelPatternParser(final DetailLevelPatternParser parser)
    {
        this.detailLevelPatternParser = parser;
    }

    private void update(final boolean changed)
    {
        // has there been a change in tile sets?
        boolean detailLevelChanged = false;
        // if detail level is locked, do not change tile sets
        if (!this.detailLevelLocked)
        {
            // get the most appropriate detail level for the current scale
            // as determined by the selection method
            DetailLevel matchingLevel = null;
            switch (this.selectionMethod)
            {
                case METHOD_CLOSEST:
                    matchingLevel = this.detailLevels.findClosest(this.getScale());
                    break;

                case METHOD_RANGES:
                    matchingLevel = this.detailLevels.findByDefinedRange(this.getScale());
                    break;

                case METHOD_DEFAULT:
                default:
                    matchingLevel = this.detailLevels.find(this.getScale());
                    break;
            }
            // if one is found (if any tile sets are registered)
            if (matchingLevel != null)
            {
                // is it the same as the one being used?
                detailLevelChanged = !matchingLevel.equals(this.currentDetailLevel);
                // update current detail level
                this.currentDetailLevel = matchingLevel;
            }
        }
        // update scaled values
        this.scaledWidth = (int) (this.getWidth() * this.getScale());
        this.scaledHeight = (int) (this.getHeight() * this.getScale());
        // broadcast scale change
        if (changed)
        {
            for (final DetailLevelEventListener listener : this.detailLevelEventListeners)
            {
                listener.onDetailScaleChanged(this.getScale());
            }
        }
        // if there's a change in detail, update appropriate values
        if (detailLevelChanged)
        {
            // notify all interested parties
            for (final DetailLevelEventListener listener : this.detailLevelEventListeners)
            {
                listener.onDetailLevelChanged();
            }
        }
    }

    public void lockDetailLevel()
    {
        this.detailLevelLocked = true;
    }

    public void unlockDetailLevel()
    {
        this.detailLevelLocked = false;
    }

    public void addDetailLevelEventListener(final DetailLevelEventListener l)
    {
        this.detailLevelEventListeners.add(l);
    }

    public void removeDetailLevelEventListener(final DetailLevelEventListener l)
    {
        this.detailLevelEventListeners.remove(l);
    }

    public void addDetailLevelSetupListener(final DetailLevelSetupListener l)
    {
        this.detailLevelSetupListeners.add(l);
    }

    public void removeDetailLevelSetupListener(final DetailLevelSetupListener l)
    {
        this.detailLevelSetupListeners.remove(l);
    }

    private void addDetailLevel(final DetailLevel detailLevel)
    {
        this.detailLevels.addDetailLevel(detailLevel);
        this.update(false);
        for (final DetailLevelSetupListener listener : this.detailLevelSetupListeners)
        {
            listener.onDetailLevelAdded();
        }
    }

    public void addDetailLevel(final float scale, final String pattern, final String downsample)
    {
        final DetailLevel detailLevel = new DetailLevel(this, scale, pattern, downsample);
        this.addDetailLevel(detailLevel);
    }

    public void addDetailLevel(final float scale, final String pattern, final String downsample, final int tileWidth,
                    final int tileHeight)
    {
        final DetailLevel detailLevel = new DetailLevel(this, scale, pattern, downsample, tileWidth, tileHeight);
        this.addDetailLevel(detailLevel);
    }

    public void addDetailLevel(final float scale, final String pattern, final String downsample, final int tileWidth,
                    final int tileHeight, final double scaleMin, final double scaleMax)
    {
        final DetailLevel detailLevel = new DetailLevel(this, scale, pattern, downsample, tileWidth, tileHeight,
                        scaleMin, scaleMax);
        this.addDetailLevel(detailLevel);
    }

    public void resetDetailLevels()
    {
        this.detailLevels.clear();
        this.update(false);
    }

    public DetailLevel getCurrentDetailLevel()
    {
        return this.currentDetailLevel;
    }

    public double getCurrentDetailLevelScale()
    {
        if (this.currentDetailLevel != null)
        {
            return this.currentDetailLevel.getScale();
        }
        return 1;
    }

    public double getHistoricalScale()
    {
        return this.historicalScale;
    }

    public void saveHistoricalScale()
    {
        this.historicalScale = this.scale;
    }

    public void setTileSelectionMethod(final int method)
    {
        this.selectionMethod = method;
    }

}