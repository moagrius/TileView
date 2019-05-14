package com.moagrius;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.moagrius.tileview.TileView;
import com.moagrius.tileview.io.StreamProviderHttp;
import com.moagrius.tileview.plugins.LowFidelityBackgroundPlugin;

public class TileViewDemoHttp extends TileViewDemoActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_tileview);
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = Bitmap.Config.RGB_565;
    Bitmap background = BitmapFactory.decodeResource(getResources(), R.drawable.downsample, options);
    TileView tileView = findViewById(R.id.tileview);
    new TileView.Builder(tileView)
        .setSize(17934, 13452)
        .setStreamProvider(new StreamProviderHttp())
        .installPlugin(new LowFidelityBackgroundPlugin(background))
        .setDiskCachePolicity(TileView.DiskCachePolicy.CACHE_ALL)
        .defineZoomLevel("https://raw.githubusercontent.com/moagrius/tv4/master/demo/src/main/assets/tiles/phi-1000000-%1$d_%2$d.jpg")
        .defineZoomLevel(1, "https://raw.githubusercontent.com/moagrius/tv4/master/demo/src/main/assets/tiles/phi-500000-%1$d_%2$d.jpg")
        .defineZoomLevel(2, "https://raw.githubusercontent.com/moagrius/tv4/master/demo/src/main/assets/tiles/phi-250000-%1$d_%2$d.jpg")
        .build();
  }

}
