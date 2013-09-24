package com.qozix.tileview.detail;

import java.util.Collections;
import java.util.LinkedList;

/*
 * This is termed "Set" while it's actually a list.
 * We need a unique, sorted collection (Set), but must
 * support frequent use of get().  NavigableSet is not
 * an option for the legacy API's we're supporting.
 * For now, use a LinkedList with Set-like behavior
 * built in.
 */

public class DetailLevelSet extends LinkedList<DetailLevel>
{

    private static final long serialVersionUID = -1742428277010988084L;

    public void addDetailLevel(final DetailLevel detailLevel)
    {
        // ensure uniqueness
        if (this.contains(detailLevel))
        {
            return;
        }
        // add to the collection
        this.add(detailLevel);
        // sort it
        Collections.sort(this);
    }

    /**
     * get the DetailLevel next largest scale (this is the default method)
     * 
     * (this could be handled by .floor with a higher API)
     * 
     * @param scale
     * @return
     */
    public DetailLevel find(final double scale)
    {
        // fast-fail
        if (this.size() == 0)
        {
            return null;
        }
        // set to null initially, but should never fail to populate
        DetailLevel match = null;
        // start at the last index
        final int index = this.size() - 1;
        // loop from largest to smallest
        for (int i = index; i >= 0; i--)
        {
            // store the iteration level in the return product for now
            match = this.get(i);
            // if the iteration scale is less than the desired scale...
            if (match.getScale() < scale)
            {
                // and there's a level registered with a larger scale
                if (i < index)
                {
                    // ... try to get the next largest
                    match = this.get(i + 1);
                    // if we're at the largest level and can't go up one, then
                    // we've got our best-case
                }
                // we've got a match, all done
                break;
            }
        }
        return match;
    }

    /**
     * Find scale that is closest to the requested scale
     * 
     * @param scale
     * @return
     */
    public DetailLevel findClosest(final double scale)
    {
        // fast-fail
        if (this.size() == 0)
        {
            return null;
        }

        // default to first item
        DetailLevel match = null;
        double diffToMatch = 0d;

        // loop through and find the "closest"
        for (final DetailLevel thisLevel : this)
        {
            if (match == null)
            {
                // default to the first value found
                match = thisLevel;
                diffToMatch = Math.abs(scale - match.getScale());
                continue;
            }

            // calculate the diff from current level to requested scale
            final double diffToCurrent = Math.abs(scale - thisLevel.getScale());
            if (diffToCurrent < diffToMatch)
            {
                match = thisLevel;
                diffToMatch = diffToCurrent;
            }
        }

        return match;
    }

    /**
     * Find by defined scale ranges
     * 
     * @see DetailLevel#setRange(min, max)
     * 
     * @param scale
     * @return
     */
    public DetailLevel findByDefinedRange(final double scale)
    {

        // fast-fail
        if (this.size() == 0)
        {
            return null;
        }

        // loop through and find a set where this scale fits
        for (final DetailLevel thisLevel : this)
        {
            if ((thisLevel.getScaleMin() <= scale) && (thisLevel.getScaleMax() >= scale))
            {
                return thisLevel;
            }
        }

        // no matches found, no default
        return null;
    }

}