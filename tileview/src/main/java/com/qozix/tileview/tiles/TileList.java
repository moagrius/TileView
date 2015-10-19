package com.qozix.tileview.tiles;

import java.util.LinkedList;

/**
 * @author Mike Dunn, 10/19/15.
 */
public class TileList extends LinkedList<Tile> {

  private int rowStart;
  private int rowEnd;
  private int columnStart;
  private int columnEnd;

  public boolean equals( Object o ){
    if( o instanceof TileList ) {
      TileList tileList = (TileList) o;
      return rowStart == tileList.rowStart
        && rowEnd == tileList.rowEnd
        && columnStart == tileList.columnStart
        && columnEnd == tileList.columnEnd;
    }
    return false;
  }

}
