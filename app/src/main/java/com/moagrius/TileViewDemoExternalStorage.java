package com.moagrius;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import com.moagrius.tileview.TileView;
import com.moagrius.tileview.io.StreamProviderFiles;

import java.io.File;
import java.util.Arrays;

public class TileViewDemoExternalStorage extends TileViewDemoActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_demos_tileview);
    TileView tileView = findViewById(R.id.tileview);
    // "/storage/emulated/0/Android/data/com.moagrius/files/phi-1000000-%1$d_%2$d.jpg"
    File sdcard = Environment.getExternalStorageDirectory();
    Log.d("TV", "directory=" + sdcard.getAbsolutePath());
    Log.d("TV", "files=" + Arrays.toString(sdcard.list()));
    new TileView.Builder(tileView)
        .setSize(17934, 13452)
        .setStreamProvider(new StreamProviderFiles())
        .defineZoomLevel(sdcard.getAbsolutePath() + "/phi-1000000-%1$d_%2$d.jpg")
        .addReadyListener(this)
        .build();
  }

}
