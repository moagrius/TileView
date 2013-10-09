package com.qozix.tileview.tiles.selection;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.qozix.tileview.detail.DetailLevel;
import com.qozix.tileview.detail.DetailLevelSet;

public class TileSelectionByRange implements ITileSelection {

    private List<Double> switchPoint = new ArrayList<Double>();

    @Override
    public DetailLevel find(double scale, DetailLevelSet levels) {
	int totalLevels = levels.size();
	int totalSwitches = this.switchPoint.size();
	
	// fast-fail
	if (totalLevels == 0) {
	    return null;
	}

	// sanity check the switchPoints with the levels
	// switchPoints should be 1 less then the total levels
	if (totalLevels != (totalSwitches + 1)) {
	    return null;
	}

	// loop through and find a set where this scale fits
	for (int index = 0; index < totalSwitches; index++) {
	    double thisSwitchPoint = this.switchPoint.get(index);

	    // when we exceed the scale we take the previous
	    if (scale < thisSwitchPoint) {
		return levels.get(index);
	    }
	}

	// take the last
	return levels.get(totalLevels - 1);
    }

    public void add(final double value) {
	this.switchPoint.add(value);
    }
}
