package com.qozix.tileview.hotspots;

import android.graphics.Rect;
import android.graphics.Region;

public class HotSpot extends Region {
	
	private Object mTag;
	private HotSpotEventListener mHotSpotEventListener;
	
	public HotSpot() {
		super();
	}

	public HotSpot( int left, int top, int right, int bottom ) {
		super( left, top, right, bottom );
	}

	public HotSpot( Rect rect ) {
		super( rect );
	}

	public HotSpot( Region region ) {
		super( region );
	}
	
	public Object getTag(){
		return mTag;
	}
	
	public void setTag( Object object ) {
		mTag = object;
	}
	
	public void setHotSpotEventListener( HotSpotEventListener hotSpotEventListener ) {
		mHotSpotEventListener = hotSpotEventListener;
	}
	
	public HotSpotEventListener getHotSpotEventListener() { 
		return mHotSpotEventListener;
	}
	
}
