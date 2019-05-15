package com.moagrius.tileview.plugins;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.moagrius.tileview.TileView;

public class LowFidelityBackgroundPlugin implements TileView.Plugin, TileView.Listener {

  private Bitmap mBitmap;
  private ImageView mImageView;
  private TileView mTileView;
  private Matrix mMatrix = new Matrix();

  public LowFidelityBackgroundPlugin(Bitmap bitmap) {
    mBitmap = bitmap;
  }

  @Override
  public void install(TileView tileView) {
    mTileView = tileView;
    mImageView = new ImageView(tileView.getContext());
    mImageView.setImageBitmap(mBitmap);
    mImageView.setPivotX(0);
    mImageView.setPivotY(0);
    ((ViewGroup) tileView.getChildAt(0)).getChildAt(0).setAlpha(0.5f);
    tileView.addView(mImageView, 0);
    tileView.addListener(this);

  }

  @Override
  public void onScaleChanged(float scale, float previous) {
//    ViewGroup.LayoutParams lp = mImageView.getLayoutParams();
//    lp.width = mTileView.getScaledWidth();
//    lp.height = mTileView.getScaledHeight();
    mImageView.setScaleX(scale);
    mImageView.setScaleY(scale);
  }

}
