package com.moagrius.tileview.plugins;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.moagrius.tileview.TileView;

public class LowFidelityBackgroundPlugin implements TileView.Plugin, TileView.Listener {

  private Bitmap mBitmap;
  private ImageView mImageView;

  public LowFidelityBackgroundPlugin(Bitmap bitmap) {
    mBitmap = bitmap;
  }

  @Override
  public void install(TileView tileView) {
    mImageView = new ImageView(tileView.getContext());
    mImageView.setImageBitmap(mBitmap);
    mImageView.setPivotX(0);
    mImageView.setPivotY(0);
    tileView.addView(mImageView, 0);
    tileView.addListener(this);
  }

  @Override
  public void onScaleChanged(float scale, float previous) {
    mImageView.setScaleX(scale);
    mImageView.setScaleY(scale);
  }

}
