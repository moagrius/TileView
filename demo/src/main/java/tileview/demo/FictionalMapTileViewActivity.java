package tileview.demo;

import android.os.Bundle;
import android.widget.ImageView;

import com.qozix.tileview.TileView;

public class FictionalMapTileViewActivity extends TileViewActivity {

	@Override
	public void onCreate( Bundle savedInstanceState ) {
		
		super.onCreate( savedInstanceState );
		
		// multiple references
		TileView tileView = getTileView();
		
		// size of original image at 100% mScale
		tileView.setSize( 4015, 4057 );

		// we're running from assets, should be fairly fast decodes, go ahead and render asap
		tileView.setShouldRenderWhilePanning( true );
		
		// detail levels
		tileView.addDetailLevel( 1.000f, "tiles/fantasy/1000/%d_%d.jpg");
		tileView.addDetailLevel( 0.500f, "tiles/fantasy/500/%d_%d.jpg");
		tileView.addDetailLevel( 0.250f, "tiles/fantasy/250/%d_%d.jpg");
		tileView.addDetailLevel( 0.125f, "tiles/fantasy/125/%d_%d.jpg" );
		
		// allow scaling past original size
		tileView.setScaleLimits( 0, 2 );
		
		// lets center all markers both horizontally and vertically
		tileView.setMarkerAnchorPoints( -0.5f, -0.5f );
		
		// individual markers
		placeMarker( R.drawable.fantasy_elves, 1616, 1353 );
		placeMarker( R.drawable.fantasy_humans, 2311, 2637 );
		placeMarker( R.drawable.fantasy_dwarves, 2104, 701 );
		placeMarker( R.drawable.fantasy_rohan, 2108, 1832 );
		placeMarker( R.drawable.fantasy_troll, 3267, 1896 );
		
		// frame the troll
		frameTo( 3267, 1896 );

	}
	
	private void placeMarker( int resId, double x, double y ) {
		ImageView imageView = new ImageView( this );
		imageView.setImageResource( resId );
		getTileView().addMarker( imageView, x, y, null, null );
	}
	
}
