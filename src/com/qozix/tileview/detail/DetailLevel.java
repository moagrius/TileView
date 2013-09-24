package com.qozix.tileview.detail;

import java.util.LinkedList;

import android.graphics.Rect;

import com.qozix.tileview.tiles.Tile;

public class DetailLevel implements Comparable<DetailLevel>
{

    private static final int    DEFAULT_TILE_SIZE = 256;

    private final double        scale;

    private int                 tileWidth         = DetailLevel.DEFAULT_TILE_SIZE;
    private int                 tileHeight        = DetailLevel.DEFAULT_TILE_SIZE;

    private final String        pattern;
    private final String        downsample;

    private final DetailManager detailManager;
    private final Rect          viewport          = new Rect();

    // range settings for ranged based selection of tiles
    private double              scaleMin          = Double.MIN_VALUE;
    private double              scaleMax          = Double.MAX_VALUE;

    public DetailLevel(final DetailManager zm, final float s, final String p, final String d, final int tw, final int th)
    {
        this.detailManager = zm;
        this.scale = s;
        this.pattern = p;
        this.downsample = d;
        this.tileWidth = tw;
        this.tileHeight = th;
    }

    public DetailLevel(final DetailManager zm, final float s, final String p, final String d, final int tw,
                    final int th, final double scaleMin, final double scaleMax)
    {
        this.detailManager = zm;
        this.scale = s;
        this.pattern = p;
        this.downsample = d;
        this.tileWidth = tw;
        this.tileHeight = th;
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
    }

    public DetailLevel(final DetailManager zm, final float s, final String p, final String d)
    {
        this(zm, s, p, d, DetailLevel.DEFAULT_TILE_SIZE, DetailLevel.DEFAULT_TILE_SIZE);
    }

    public LinkedList<Tile> getIntersections()
    {

        final double relativeScale = this.getRelativeScale();

        final int drawableWidth = (int) (this.detailManager.getWidth() * this.getScale() * relativeScale);
        final int drawableHeight = (int) (this.detailManager.getHeight() * this.getScale() * relativeScale);
        final double offsetWidth = (this.tileWidth * relativeScale);
        final double offsetHeight = (this.tileHeight * relativeScale);

        final LinkedList<Tile> intersections = new LinkedList<Tile>();

        this.viewport.set(this.detailManager.getComputedViewport());

        // TODO test if mins are right
        this.viewport.top = Math.max(this.viewport.top, 0);
        this.viewport.left = Math.max(this.viewport.left, 0);
        this.viewport.right = Math.min(this.viewport.right, drawableWidth);
        this.viewport.bottom = Math.min(this.viewport.bottom, drawableHeight);

        final int startingRow = (int) Math.floor(this.viewport.top / offsetHeight);
        final int endingRow = (int) Math.ceil(this.viewport.bottom / offsetHeight);
        final int startingColumn = (int) Math.floor(this.viewport.left / offsetWidth);
        final int endingColumn = (int) Math.ceil(this.viewport.right / offsetWidth);

        final DetailLevelPatternParser parser = this.detailManager.getDetailLevelPatternParser();

        for (int iterationRow = startingRow; iterationRow < endingRow; iterationRow++)
        {
            for (int iterationColumn = startingColumn; iterationColumn < endingColumn; iterationColumn++)
            {
                final String fileName = parser.parse(this.pattern, iterationRow, iterationColumn);
                final int left = iterationColumn * this.tileWidth;
                final int top = iterationRow * this.tileHeight;
                final Tile tile = new Tile(left, top, this.tileWidth, this.tileHeight, fileName);
                intersections.add(tile);
            }
        }

        return intersections;

    }

    /**
     * Set the range that this set of tiles is used.
     * 
     * @param scaleMin
     * @param scaleMax
     */
    public void setRanger(final double scaleMin, final double scaleMax)
    {
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
    }

    public double getScale()
    {
        return this.scale;
    }

    public double getRelativeScale()
    {
        return this.detailManager.getScale() / this.scale;
    }

    public int getTileWidth()
    {
        return this.tileWidth;
    }

    public int getTileHeight()
    {
        return this.tileHeight;
    }

    public String getPattern()
    {
        return this.pattern;
    }

    public String getDownsample()
    {
        return this.downsample;
    }

    @Override
    public int compareTo(final DetailLevel o)
    {
        return (int) Math.signum(this.getScale() - o.getScale());
    }

    @Override
    public boolean equals(final Object o)
    {
        if (o instanceof DetailLevel)
        {
            final DetailLevel zl = (DetailLevel) o;
            return (zl.getScale() == this.getScale());
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        final long bits = (Double.doubleToLongBits(this.getScale()) * 43);
        return (((int) bits) ^ ((int) (bits >> 32)));
    }

    public double getScaleMin()
    {
        return this.scaleMin;
    }

    public double getScaleMax()
    {
        return this.scaleMax;
    }

}