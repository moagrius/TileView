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

public class DetailLevelSet extends LinkedList<DetailLevel> {

	private static final long serialVersionUID = -1742428277010988084L;

	public void addDetailLevel( DetailLevel detailLevel ) {
		// ensure uniqueness
		if ( contains( detailLevel ) ) {
			return;
		}
		// add to the collection
		add( detailLevel );
		// sort it
		Collections.sort( this );
	}

	// get the DetailLevel closest to the passed scale - this could be handled by .floor with a higher API
	public DetailLevel find( double scale ) {
		// fast-fail
		if ( size() == 0 ) {
			return null;
		}
		// set to null initially, but should never fail to populate
		DetailLevel match = null;
		// start at the last index
		int index = size() - 1;
		// loop from largest to smallest
		for ( int i = index; i >= 0; i-- ) {
			// store the iteration level in the return product for now
			match = get( i );
			// if the iteration scale is less than the desired scale...
			if ( match.getScale() < scale ) {
				// and there's a level registered with a larger scale
				if ( i < index ) {
					// ... try to get the next largest
					match = get( i + 1 );
					// if we're at the largest level and can't go up one, then we've got our best-case
				}
				// we've got a match, all done
				break;
			}
		}
		return match;
	}
	

}

