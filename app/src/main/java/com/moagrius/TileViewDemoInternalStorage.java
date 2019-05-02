package com.moagrius;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.moagrius.tileview.TileView;

public class TileViewDemoInternalStorage extends Activity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_tileview);
    TileView tileView = findViewById(R.id.tileview);
    new TileView.Builder(tileView).setSize(17934, 13452).defineZoomLevel("/data/user/0/com.moagrius.demo/files/phi-1000000-%1$d_%2$d.jpg").build();

  }

}
