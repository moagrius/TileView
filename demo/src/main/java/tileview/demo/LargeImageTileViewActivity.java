package tileview.demo;

import com.qozix.tileview.TileView;

import android.os.Bundle;

public class LargeImageTileViewActivity extends TileViewActivity {

	@Override
	public void onCreate( Bundle savedInstanceState ) {
		
		super.onCreate( savedInstanceState );
		
		// multiple references
		TileView tileView = getTileView();
		
		// by disabling transitions, we won't see a flicker of background color when moving between tile sets
		tileView.setTransitionsEnabled( false );
		
		// size of original image at 100% scale
		tileView.setSize( 2835, 4289 );
		
		// detail levels
		tileView.addDetailLevel( 1.000f, "tiles/painting/1000/%col%_%row%.jpg");
		tileView.addDetailLevel( 0.500f, "tiles/painting/500/%col%_%row%.jpg");
		tileView.addDetailLevel( 0.250f, "tiles/painting/250/%col%_%row%.jpg");
		tileView.addDetailLevel( 0.125f, "tiles/painting/125/%col%_%row%.jpg");
		
		// set scale to 0, but keep scaleToFit true, so it'll be as small as possible but still match the container
		tileView.setScale( 0 );
		
		// let's use 0-1 positioning...
		tileView.defineRelativeBounds( 0, 0, 1,  1 );
		
		// frame to center
		frameTo( 0.5, 0.5 );
		
	}
}
