package com.moagrius;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.moagrius.tileview.TileView;
import com.moagrius.tileview.io.StreamProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class TileViewDemoAssets extends TileViewDemoActivity {

  private static class SlowStreamProviderAssets implements StreamProvider {
    @Override
    public InputStream getStream(int column, int row, Context context, Object data) throws IOException {
      String file = String.format(Locale.US, (String) data, column, row);
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return context.getAssets().open(file);
    }
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_tileview);

    Bitmap background = BitmapFactory.decodeResource(getResources(), R.drawable.downsample);

    TileView tileView = findViewById(R.id.tileview);
    tileView.setBackgroundColor(Color.RED);
    tileView.getChildAt(0).setBackgroundColor(Color.GREEN);
    tileView.setScaleLimits(0f, 10f);
    //tileView.setMinimumScaleMode(ScalingScrollView.MinimumScaleMode.CONTAIN);
    // TODO: included for debug
    // tileView.setMinimumScaleMode(ScalingScrollView.MinimumScaleMode.COVER);
    // tileView.setMinimumScaleMode(ScalingScrollView.MinimumScaleMode.NONE);
    new TileView.Builder(tileView)
        .setSize(17934, 13452)
        .defineZoomLevel("tiles/phi-1000000-%1$d_%2$d.jpg")
        //.installPlugin(new LowFidelityBackgroundPlugin(background))
        .build();

  }

}
