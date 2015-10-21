package tileview.demo;

import android.app.Activity;
import android.content.Context;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.qozix.tileview.TileView;

import java.util.ArrayList;

public class RealMapTileViewActivity extends Activity {

  public static final double NORTH_WEST_LATITUDE = 39.9639998777094;
  public static final double NORTH_WEST_LONGITUDE = -75.17261900652977;
  public static final double SOUTH_EAST_LATITUDE = 39.93699709962642;
  public static final double SOUTH_EAST_LONGITUDE = -75.12462846235614;

  private TileView tileView;

  private static class NoDoubleTapTileView extends TileView {
    public NoDoubleTapTileView( Context context ) {
      super( context );
    }

    public boolean onDoubleTap( MotionEvent event ) {
      return false;
    }
  }

  @Override
  public void onCreate( Bundle savedInstanceState ) {

    super.onCreate( savedInstanceState );

    tileView = new TileView( this );

    tileView.setSize( 8967, 6726 );

    tileView.setBackgroundColor( 0xFFe7e7e7 );

    tileView.addDetailLevel( 0.1250f, "tiles/map/phi-62500-%d_%d.jpg" );
    tileView.addDetailLevel( 0.2500f, "tiles/map/phi-125000-%d_%d.jpg" );
    tileView.addDetailLevel( 0.5000f, "tiles/map/phi-250000-%d_%d.jpg" );
    tileView.addDetailLevel( 1.0000f, "tiles/map/phi-500000-%d_%d.jpg" );

    tileView.setMarkerAnchorPoints( -0.5f, -1.0f );

    tileView.defineBounds(
      NORTH_WEST_LONGITUDE,
      NORTH_WEST_LATITUDE,
      SOUTH_EAST_LONGITUDE,
      SOUTH_EAST_LATITUDE
    );

    DisplayMetrics metrics = getResources().getDisplayMetrics();
    Paint paint = tileView.getPathPaint();
    paint.setShadowLayer(
      TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 4, metrics ),
      TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 2, metrics ),
      TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 2, metrics ),
      0x66000000
    );
    paint.setColor( 0x883399FF );
    paint.setStrokeWidth( TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 4, metrics ) );
    paint.setPathEffect(
      new CornerPathEffect(
        TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 5, metrics )
      )
    );
    tileView.drawPath( points.subList( 1, 5 ), null );

    for( double[] point : points ) {
      ImageView marker = new ImageView( this );
      marker.setTag( point );
      marker.setImageResource( Math.random() < 0.75 ? R.drawable.map_marker_normal : R.drawable.map_marker_featured );
      marker.setOnClickListener( markerClickListener );
      tileView.addMarker( marker, point[0], point[1], null, null );
    }

    tileView.setTransitionsEnabled( false );

    ImageView downsample = new ImageView( this );
    downsample.setImageResource( R.drawable.downsample );
    //tileView.addView( downsample, 0 );

    Button button = new Button( this );
    button.setText( "ZoomAndScale" );
    button.setOnClickListener( new View.OnClickListener() {
      @Override
      public void onClick( View view ) {
        /*
        double[] spot = points.get( 0 );
        tileView.slideToAndCenterWithScale( spot[0], spot[1], 0.5f );
        */
        tileView.setScaleLimits( 0.1f, 1.0f );
        //tileView.setShouldScaleToFit( false );
        tileView.setScale( 0.1f );
      }
    } );


    RelativeLayout contentView = new RelativeLayout( this );
    contentView.addView( tileView );
    contentView.addView( button );

    setContentView( contentView );

    /*
    LinearLayout linearLayout = new LinearLayout( this );
		linearLayout.setOrientation( LinearLayout.VERTICAL );

		View spacer = new View( this );
		LayoutParams spacerLayoutParams =  new LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT );
		spacerLayoutParams.weight = 1;
		linearLayout.addView( spacer, spacerLayoutParams );

		LayoutParams tileViewLayoutParams = new LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT );
		tileViewLayoutParams.weight = 1;
		linearLayout.addView( tileView, tileViewLayoutParams );

		setContentView( linearLayout );

		*/

  }

  private View.OnClickListener markerClickListener = new View.OnClickListener() {

    @Override
    public void onClick( View view ) {
      double[] position = (double[]) view.getTag();
      tileView.slideToAndCenter( position[0], position[1] );
      SampleCallout callout = new SampleCallout( view.getContext() );
      tileView.addCallout( callout, position[0], position[1], -0.5f, -1.0f );
      callout.transitionIn();
      callout.setTitle( "MAP CALLOUT" );
      callout.setSubtitle( "Info window at coordinate:\n" + position[1] + ", " + position[0] );
    }
  };

  // if you want the bottoms of your markers to not show, sort them by y position
  // a list of points to demonstrate markers and paths
  private ArrayList<double[]> points = new ArrayList<>();
  {
    points.add( new double[] {-75.1489070, 39.9484760} );
    points.add( new double[] {-75.1494000, 39.9487722} );
    points.add( new double[] {-75.1468350, 39.9474180} );
    points.add( new double[] {-75.1472000, 39.9482000} );
    points.add( new double[] {-75.1437980, 39.9508290} );
    points.add( new double[] {-75.1479650, 39.9523130} );
    points.add( new double[] {-75.1445500, 39.9472960} );
    points.add( new double[] {-75.1506100, 39.9490630} );
    points.add( new double[] {-75.1521278, 39.9508083} );
    points.add( new double[] {-75.1477600, 39.9475320} );
    points.add( new double[] {-75.1503800, 39.9489900} );
    points.add( new double[] {-75.1464200, 39.9482000} );
    points.add( new double[] {-75.1464850, 39.9498500} );
    points.add( new double[] {-75.1487030, 39.9524300} );
    points.add( new double[] {-75.1500167, 39.9488750} );
    points.add( new double[] {-75.1458360, 39.9479700} );
    points.add( new double[] {-75.1498222, 39.9515389} );
    points.add( new double[] {-75.1501990, 39.9498900} );
    points.add( new double[] {-75.1460060, 39.9474210} );
    points.add( new double[] {-75.1490230, 39.9533960} );
    points.add( new double[] {-75.1471980, 39.9485350} );
    points.add( new double[] {-75.1493500, 39.9490200} );
    points.add( new double[] {-75.1500910, 39.9503850} );
    points.add( new double[] {-75.1483930, 39.9485040} );
    points.add( new double[] {-75.1517260, 39.9473720} );
    points.add( new double[] {-75.1525630, 39.9471360} );
    points.add( new double[] {-75.1438400, 39.9473390} );
    points.add( new double[] {-75.1468240, 39.9495400} );
    points.add( new double[] {-75.1466410, 39.9499900} );
    points.add( new double[] {-75.1465050, 39.9501110} );
    points.add( new double[] {-75.1473460, 39.9436200} );
    points.add( new double[] {-75.1501570, 39.9480430} );
  }
}
