package tileview.demo;

import android.os.Bundle;

import com.qozix.tileview.TileView;

public class LargeImageTileViewActivity extends TileViewActivity {

	@Override
	public void onCreate( Bundle savedInstanceState ) {
		
		super.onCreate( savedInstanceState );
		
		// multiple references
		TileView tileView = getTileView();

		// let the image explode
		tileView.setScaleLimits( 0, 2 );
		
		// size of original image at 100% mScale
		tileView.setSize( 2835, 4289 );
		
		// detail levels
		tileView.addDetailLevel( 1.000f, "tiles/painting/1000/%d_%d.jpg");
		tileView.addDetailLevel( 0.500f, "tiles/painting/500/%d_%d.jpg");
		tileView.addDetailLevel( 0.250f, "tiles/painting/250/%d_%d.jpg");
		tileView.addDetailLevel( 0.125f, "tiles/painting/125/%d_%d.jpg");
		
		// set mScale to 0, but keep scaleToFit true, so it'll be as small as possible but still match the container
		tileView.setScale( 0 );
		
		// let's use 0-1 positioning...
		tileView.defineBounds( 0, 0, 1, 1 );
		
		// frame to center
		frameTo( 0.5, 0.5 );

		// render while panning
		tileView.setShouldRenderWhilePanning( true );

		// disallow going back to minimum scale while double-taping at maximum scale (for demo purpose)
		tileView.setShouldLoopScale( false );
	}
}
