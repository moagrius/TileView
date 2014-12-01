package com.qozix.tileview.samples;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

import com.qozix.os.AsyncTask;
import com.qozix.tileview.detail.DetailLevel;
import com.qozix.tileview.detail.DetailLevelEventListener;
import com.qozix.tileview.detail.DetailManager;
import com.qozix.tileview.graphics.BitmapDecoder;
import com.qozix.tileview.graphics.BitmapDecoderAssets;

public class SampleManager extends View implements DetailLevelEventListener {

	private DetailManager detailManager;
	private BitmapDecoder decoder = new BitmapDecoderAssets();
	
	private Rect area = new Rect(0, 0, 0, 0);
	
	private Bitmap bitmap;
	private String lastFileName;
	private String currentFileName;
	
	public SampleManager( Context context, DetailManager dm ) {
		
		super( context );
		
		detailManager = dm;
		detailManager.addDetailLevelEventListener( this );
		
		update();
		
	}
	
	public void setDecoder( BitmapDecoder d ){
		decoder = d;
	}
	
	public void clear(){
		bitmap = null;
		lastFileName = null;
	}

	private class BitmapDecodeTask implements Runnable {
	    @Override
	    public void run() {
	        bitmap = decoder.decode(currentFileName, getContext());
	        postInvalidate();
	    }
	}

	private BitmapDecodeTask decodeTask = new BitmapDecodeTask();

	public void update() {
		DetailLevel detailLevel = detailManager.getCurrentDetailLevel();
		if( detailLevel != null ) {
			String fileName = detailLevel.getDownsample();
			if( fileName != null ) {
				if( !fileName.equals( lastFileName ) ) {
					currentFileName = fileName;
					AsyncTask.execute(decodeTask);
				}
			}
			lastFileName = fileName;
		}		
	}
	
	@Override
	public void onDetailLevelChanged() {
		update();
	}

	@Override
	public void onDetailScaleChanged( double s ) {
		
	}

	@Override
	public void onDraw( Canvas canvas ) {
		if( bitmap != null) {
			area.right = getWidth();
			area.bottom = getHeight();
			canvas.drawBitmap( bitmap, null, area, null);
		}		
		super.onDraw( canvas );
	}
}
