package com.qozix.tileview.tiles;

import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Mike Dunn, 2/28/16.
 */
public class TileManager {

  public Set<Tile> tilesInCurrentViewport = (Set<Tile>) Collections.synchronizedSet(new HashSet<Tile>());
  public Set<Tile> tilesNotInCurrentViewport = (Set<Tile>) Collections.synchronizedSet(new HashSet<Tile>());
  public Set<Tile> tilesAlreadyRendered = (Set<Tile>) Collections.synchronizedSet(new HashSet<Tile>());

  /**
   * Effectively adds any new tiles, without replacing existing tiles, and removes those not in passed set.
   * @param recentlyComputedVisibleTileSet
   */
  public void reconcile( Set<Tile> recentlyComputedVisibleTileSet ){
    Log.d( "DEBUG", "tile size passed: " + recentlyComputedVisibleTileSet.size() );
    Log.d( "DEBUG", "tile size existing: " + tilesInCurrentViewport.size() );
    for( Tile tile : tilesInCurrentViewport ) {
      if( !recentlyComputedVisibleTileSet.contains( tile ) ) {
        tilesNotInCurrentViewport.add( tile );
      }
    }
    Log.d( "DEBUG", "tile not in viewport size: " + tilesNotInCurrentViewport.size() );
    tilesInCurrentViewport.addAll( recentlyComputedVisibleTileSet );
    Log.d( "DEBUG", "tile size after add all: " + tilesInCurrentViewport.size() );
    tilesInCurrentViewport.removeAll( tilesNotInCurrentViewport );
    Log.d( "DEBUG", "tile size after remove all: " + tilesInCurrentViewport.size() );
    // TODO: this?
    tilesNotInCurrentViewport.clear();
  }

}
