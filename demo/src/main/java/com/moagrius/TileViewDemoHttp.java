package com.moagrius;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.moagrius.tileview.TileView;
import com.moagrius.tileview.io.StreamProviderHttp;

public class TileViewDemoHttp extends TileViewDemoActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_tileview);
    TileView tileView = findViewById(R.id.tileview);
    new TileView.Builder(tileView)
        .setSize(17934, 13452)
        .setStreamProvider(new StreamProviderHttp())
        .defineZoomLevel("https://raw.githubusercontent.com/moagrius/tv4/master/app/src/main/assets/tiles/phi-1000000-%1$d_%2$d.jpg")
        .defineZoomLevel(1, "https://raw.githubusercontent.com/moagrius/tv4/master/app/src/main/assets/tiles/phi-500000-%1$d_%2$d.jpg")
        .defineZoomLevel(2, "https://raw.githubusercontent.com/moagrius/tv4/master/app/src/main/assets/tiles/phi-250000-%1$d_%2$d.jpg")
        .addReadyListener(this)
        .build();
  }

}
