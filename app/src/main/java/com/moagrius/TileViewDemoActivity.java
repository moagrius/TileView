package com.moagrius;

import android.app.Activity;

import com.moagrius.tileview.TileView;

public abstract class TileViewDemoActivity extends Activity implements TileView.ReadyListener{

  @Override
  public void onReady(TileView tileView) {
    tileView.scrollTo(tileView.getContentWidth() / 2 , tileView.getContentHeight() / 2);
  }

}
