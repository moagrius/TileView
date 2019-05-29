package com.moagrius;

import android.support.v7.app.AppCompatActivity;

import com.moagrius.tileview.TileView;

public abstract class TileViewDemoActivity extends AppCompatActivity {

  private boolean mShouldFrameToCenterOnReady;

  public void frameToCenterOnReady() {
    mShouldFrameToCenterOnReady = true;
  }

  @Override
  public void setContentView(int layoutResID) {
    super.setContentView(layoutResID);
    TileView tileView = findViewById(R.id.tileview);
    tileView.addReadyListener(this::frameToCenter);
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (isFinishing()) {
      TileView tileView = findViewById(R.id.tileview);
      tileView.destroy();
    }
  }

  public void frameToCenter(TileView tileView) {
    if (mShouldFrameToCenterOnReady) {
      tileView.post(() -> tileView.scrollTo(
          tileView.getContentWidth() / 2 - tileView.getWidth() / 2,
          tileView.getContentHeight() / 2 - tileView.getHeight() / 2
      ));
    }
  }

}
