package com.qozix.tileview.detail;

import java.util.Collections;
import java.util.LinkedList;

import com.qozix.tileview.tiles.selection.TileSelectionNoScale;
import com.qozix.tileview.tiles.selection.ITileSelection;

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

	private ITileSelection tileSelector = new TileSelectionNoScale();
	
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

	public DetailLevel find( double scale ) {
	    return this.tileSelector.find(scale, this);
	}

	public ITileSelection getTileSelection() {
	    return this.tileSelector;
	}

	/**
	 * Set the tile selection method, defaults to {@link TileSelectionNoScale}
	 * 
	 * @param selection
	 */
	public void setTileSelection(ITileSelection selection) {
	    this.tileSelector = selection;
	}
	
}

