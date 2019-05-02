package com.moagrius;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.moagrius.tileview.TileView;

public class TileViewDemoSimple extends TileViewDemoActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_tileview);

    TileView tileView = findViewById(R.id.tileview);
    new TileView.Builder(tileView)
        .setSize(17934, 13452)
        .defineZoomLevel("tiles/phi-1000000-%1$d_%2$d.jpg")
        .addReadyListener(this)
        .build();

  }

}
