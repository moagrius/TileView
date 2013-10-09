package com.qozix.tileview.tiles.selection;

import com.qozix.tileview.detail.DetailLevel;
import com.qozix.tileview.detail.DetailLevelSet;

public interface ITileSelection {
    public DetailLevel find(double scale, DetailLevelSet levels);
}
