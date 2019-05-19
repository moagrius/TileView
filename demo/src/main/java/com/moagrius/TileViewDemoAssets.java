package com.moagrius;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.moagrius.tileview.TileView;
import com.moagrius.widget.ScalingScrollView;

public class TileViewDemoAssets extends TileViewDemoActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_tileview);
    TileView tileView = findViewById(R.id.tileview);
    frameToCenterOnReady();
    tileView.setScaleLimits(0f, 10f);
    tileView.setMinimumScaleMode(ScalingScrollView.MinimumScaleMode.CONTAIN);
    new TileView.Builder(tileView)
        .setSize(16384, 13312)
        .defineZoomLevel("tiles/phi-1000000-%1$d_%2$d.jpg")
        .build();
  }

}
