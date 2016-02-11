package tileview.demo;

import android.os.Bundle;

import com.qozix.tileview.TileView;

/**
 * Created by bruno on 11/02/16.
 */
public class InternetTileViewActivity extends TileViewActivity {
    @Override
    public void onCreate( Bundle savedInstanceState ) {

        super.onCreate( savedInstanceState );

        // multiple references
        TileView tileView = getTileView();

        // simple http provider
        tileView.setBitmapProvider(new BitmapHttpProvider());

        // by disabling transitions, we won't see a flicker of background color when moving between tile sets
        tileView.setTransitionsEnabled( false );

        /*//Tile 01
        // size of original image at 100% mScale
        tileView.setSize( 3840, 2160);

        // detail levels
        tileView.addDetailLevel( 1.000f, "https://s3.amazonaws.com/tileview/tile01/100/tile_%d_%d.png");
        tileView.addDetailLevel( 0.750f, "https://s3.amazonaws.com/tileview/tile01/75/tile_%d_%d.png");
        tileView.addDetailLevel( 0.500f, "https://s3.amazonaws.com/tileview/tile01/50/tile_%d_%d.png");
        tileView.addDetailLevel( 0.250f, "https://s3.amazonaws.com/tileview/tile01/25/tile_%d_%d.png");*/

        // size of original image at 100% mScale
        tileView.setSize( 8192, 4608);

        // detail levels
        tileView.addDetailLevel( 1.000f, "https://s3.amazonaws.com/tileview/tile02/100/tile_%d_%d.jpg");
        tileView.addDetailLevel( 0.750f, "https://s3.amazonaws.com/tileview/tile02/75/tile_%d_%d.jpg");
        tileView.addDetailLevel( 0.500f, "https://s3.amazonaws.com/tileview/tile02/50/tile_%d_%d.jpg");
        tileView.addDetailLevel( 0.250f, "https://s3.amazonaws.com/tileview/tile02/25/tile_%d_%d.jpg");

        // set mScale to 0, but keep scaleToFit true, so it'll be as small as possible but still match the container
        tileView.setScale( 0 );

        // let's use 0-1 positioning...
        tileView.defineBounds( 0, 0, 1, 1 );

        // frame to center
        frameTo( 0.5, 0.5 );

        // render while panning
        tileView.setShouldRenderWhilePanning( true );

    }
}
