package com.qozix.tileview.tiles.selector;

import com.qozix.tileview.detail.DetailLevel;
import com.qozix.tileview.detail.DetailLevelSet;

public interface TileSetSelector {
    public DetailLevel find(double scale, DetailLevelSet levels);
}
