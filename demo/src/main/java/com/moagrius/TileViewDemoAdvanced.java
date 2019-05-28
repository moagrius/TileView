package com.moagrius;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.moagrius.tileview.TileView;
import com.moagrius.tileview.plugins.CoordinatePlugin;
import com.moagrius.tileview.plugins.HotSpotPlugin;
import com.moagrius.tileview.plugins.InfoWindowPlugin;
import com.moagrius.tileview.plugins.LowFidelityBackgroundPlugin;
import com.moagrius.tileview.plugins.MarkerPlugin;
import com.moagrius.tileview.plugins.PathPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Mike Dunn, 2/4/18.
 */

public class TileViewDemoAdvanced extends Activity {

  public static final double NORTH = -75.17261900652977;
  public static final double WEST = 39.9639998777094;
  public static final double SOUTH = -75.12462846235614;
  public static final double EAST = 39.93699709962642;

  private ArrayList<double[]> sites = new ArrayList<>();

  {
    sites.add(new double[]{-75.1494000, 39.9487722});
    sites.add(new double[]{-75.1468350, 39.9474180});
    sites.add(new double[]{-75.1472000, 39.9482000});
    sites.add(new double[]{-75.1437980, 39.9508290});
    sites.add(new double[]{-75.1479650, 39.9523130});
  }

  private boolean mIsRestoring;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_tileview);
    TileView tileView = findViewById(R.id.tileview);
    tileView.setScaleLimits(0, 2f);
    mIsRestoring = savedInstanceState != null;
    new TileView.Builder(tileView)
        .setSize(16384, 13056)
        .defineZoomLevel("tiles/phi-1000000-%1$d_%2$d.jpg")
        .defineZoomLevel(1, "tiles/phi-500000-%1$d_%2$d.jpg")
        .defineZoomLevel(2, "tiles/phi-250000-%1$d_%2$d.jpg")
        .installPlugin(new MarkerPlugin(this))
        .installPlugin(new InfoWindowPlugin(getInfoView()))
        .installPlugin(new CoordinatePlugin(WEST, NORTH, EAST, SOUTH))
        .installPlugin(new HotSpotPlugin())
        .installPlugin(new PathPlugin())
        .installPlugin(new LowFidelityBackgroundPlugin(getBackgroundBitmap()))
        .addReadyListener(this::onReady)
        .build();
  }

  private View getInfoView() {
    int elevation = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
    int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
    TextView infoView = new TextView(this);
    infoView.setPadding(padding, padding, padding, padding);
    infoView.setBackgroundColor(Color.WHITE);
    infoView.setGravity(Gravity.CENTER);
    infoView.setLineSpacing(0, 1.3f);
    infoView.setTextSize(11);
    ViewCompat.setElevation(infoView, elevation);
    return infoView;
  }

  public Bitmap getBackgroundBitmap() {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = Bitmap.Config.RGB_565;
    return BitmapFactory.decodeResource(getResources(), R.drawable.downsample, options);
  }

  private void onReady(TileView tileView) {

    CoordinatePlugin coordinatePlugin = tileView.getPlugin(CoordinatePlugin.class);
    InfoWindowPlugin infoWindowPlugin = tileView.getPlugin(InfoWindowPlugin.class);
    HotSpotPlugin hotSpotPlugin = tileView.getPlugin(HotSpotPlugin.class);
    MarkerPlugin markerPlugin = tileView.getPlugin(MarkerPlugin.class);

    // drop some markers, with info window expansions
    String template = "Clicked marker at:\n%1$f\n%2$f";
    View.OnClickListener markerClickListener = view -> {
      double[] coordinate = (double[]) view.getTag();
      int x = coordinatePlugin.longitudeToX(coordinate[1]);
      int y = coordinatePlugin.latitudeToY(coordinate[0]);
      tileView.smoothScrollTo(x - tileView.getWidth() / 2, y - tileView.getHeight() / 2);
      String label = String.format(Locale.US, template, coordinate[0], coordinate[1]);
      TextView infoView = infoWindowPlugin.getView();
      infoView.setText(label);
      infoWindowPlugin.show(x, y, -0.5f, -1f);
    };

    for (double[] coordinate : sites) {
      int x = coordinatePlugin.longitudeToUnscaledX(coordinate[1]);
      int y = coordinatePlugin.latitudeToUnscaledY(coordinate[0]);
      ImageView marker = new ImageView(this);
      marker.setTag(coordinate);
      marker.setImageResource(R.drawable.marker);
      marker.setOnClickListener(markerClickListener);
      markerPlugin.addMarker(marker, x, y, -0.5f, -1f, 0, 0);
    }
    markerPlugin.refreshPositions();

    // draw a path
    Paint paint = new Paint();
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(0xFF4286f4);
    paint.setStrokeWidth(0);
    paint.setAntiAlias(true);

    DisplayMetrics metrics = getResources().getDisplayMetrics();
    paint.setShadowLayer(
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, metrics),
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, metrics),
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, metrics),
        0x66000000);
    paint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, metrics));
    paint.setPathEffect(new CornerPathEffect(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, metrics)));

    List<Point> points = new ArrayList<>();
    for (double[] coordinate : sites) {
      Point point = new Point();
      point.x = coordinatePlugin.longitudeToUnscaledX(coordinate[1]);
      point.y = coordinatePlugin.latitudeToUnscaledY(coordinate[0]);
      points.add(point);
    }

    PathPlugin pathPlugin = tileView.getPlugin(PathPlugin.class);
    pathPlugin.drawPath(points, paint);

    // hotspot
    HotSpotPlugin.HotSpot hotSpot = hotSpotPlugin.addHotSpot(points, h -> Log.d("TV", "hot spot touched: " + h.getTag()));
    hotSpot.setTag("Any piece of data...");

    // frame it
    if (!mIsRestoring) {
      Log.d("TV", "is not restoring, so frame it");
      double[] coordinate = sites.get(0);
      int x = coordinatePlugin.longitudeToX(coordinate[1]);
      int y = coordinatePlugin.latitudeToY(coordinate[0]);
      tileView.scrollTo(x, y);
    } else {
      Log.d("TV", "is restoring, return to last scroll position");
      //tileView.setScale(0.55f);
      //new Handler().postDelayed(() -> tileView.setScale(0.55f), 2000);
    }

    Log.d("TV", "onReady");

  }


}
