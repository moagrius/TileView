package com.moagrius;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.moagrius.tileview.TileView;
import com.moagrius.tileview.io.StreamProviderFiles;

import java.io.File;
import java.util.Arrays;

public class TileViewDemoExternalStorage extends Activity {

  private static final int READ_REQUEST_CODE = 2;

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
    requestPermissions(permissions, READ_REQUEST_CODE);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
      case READ_REQUEST_CODE:
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
            .build();
    }
  }

}
