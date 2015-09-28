package tileview.demo;

import android.app.Activity;
import android.os.Bundle;

import com.qozix.tileview.TileView;

public class TileViewActivity extends Activity {

	private TileView tileView;
	
	@Override
	public void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		tileView = new TileView( this );
		setContentView( tileView );
	}
	
	@Override
	public void onPause() {
		super.onPause();
		tileView.clear();
	}

	@Override
	public void onResume() {
		super.onResume();
		tileView.resume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		tileView.destroy();
		tileView = null;
	}
	
	public TileView getTileView(){
		return tileView;
	}
	
	/**
	 * This is a convenience method to moveToAndCenter after layout (which won't happen if called directly in onCreate
	 * see https://github.com/moagrius/TileView/wiki/FAQ
	 */
	public void frameTo( final double x, final double y ) {
		getTileView().post( new Runnable() {
			@Override
			public void run() {
				getTileView().moveToAndCenter( x, y );
			}			
		});		
	}
}
