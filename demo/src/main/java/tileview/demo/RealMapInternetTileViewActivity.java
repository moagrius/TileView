package tileview.demo;

import android.os.Bundle;

import com.qozix.tileview.TileView;

import tileview.demo.provider.BitmapProviderPicasso;

public class RealMapInternetTileViewActivity extends TileViewActivity {
  @Override
  public void onCreate( Bundle savedInstanceState ) {

    super.onCreate( savedInstanceState );

    // multiple references
    TileView tileView = getTileView();

    // simple http provider
    tileView.setBitmapProvider( new BitmapProviderPicasso() );

    // by disabling transitions, we won't see a flicker of background color when moving between tile sets
    tileView.setTransitionsEnabled( false );

    // size and geolocation
    tileView.setSize( 8967, 6726 );

    // we won't use a downsample here, so color it similarly to tiles
    tileView.setBackgroundColor( 0xFFFF9900 );

    tileView.addDetailLevel( 0.0125f, "https://raw.githubusercontent.com/moagrius/TileView/master/demo/src/main/assets/tiles/map/phi-62500-%d_%d.jpg" );
    tileView.addDetailLevel( 0.2500f, "https://raw.githubusercontent.com/moagrius/TileView/master/demo/src/main/assets/tiles/map/phi-125000-%d_%d.jpg" );
    tileView.addDetailLevel( 0.5000f, "https://raw.githubusercontent.com/moagrius/TileView/master/demo/src/main/assets/tiles/map/phi-250000-%d_%d.jpg" );
    tileView.addDetailLevel( 1.0000f, "https://raw.githubusercontent.com/moagrius/TileView/master/demo/src/main/assets/tiles/map/phi-500000-%d_%d.jpg" );


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
