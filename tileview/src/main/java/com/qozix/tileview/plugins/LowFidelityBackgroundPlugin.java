package com.qozix.tileview.plugins;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.qozix.tileview.TileView;

public class LowFidelityBackgroundPlugin implements TileView.Plugin {

  private Bitmap mBitmap;

  public LowFidelityBackgroundPlugin(Bitmap bitmap) {
    mBitmap = bitmap;
  }

  @Override
  public void install(TileView tileView) {
    ImageView imageView = new ImageView(tileView.getContext());
    imageView.setImageBitmap(mBitmap);
    tileView.addView(imageView, 0);
  }

}
