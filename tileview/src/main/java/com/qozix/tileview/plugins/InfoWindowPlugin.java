package com.qozix.tileview.plugins;

import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.qozix.tileview.TileView;

public class InfoWindowPlugin extends FrameLayout implements TileView.Plugin, TileView.Listener, TileView.TouchListener {

  private View mView;

  public InfoWindowPlugin(View view) {
    super(view.getContext());
    mView = view;
    hide();
    if (mView.getLayoutParams() == null) {
      LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
      mView.setLayoutParams(lp);
    }
    addView(mView);
  }

  @Override
  public void install(TileView tileView) {
    tileView.addView(this);
    tileView.addListener(this);
    tileView.addTouchListener(this);
  }

  @SuppressWarnings("unchecked")
  public <T extends View> T getView() {
    return (T) mView;
  }

  public void show(int x, int y, float anchorX, float anchorY) {
    LayoutParams lp = (LayoutParams) mView.getLayoutParams();
    lp.leftMargin = (int) (x + mView.getMeasuredWidth() * anchorX);
    lp.topMargin = (int) (y + mView.getMeasuredHeight() * anchorY);
    mView.setVisibility(View.VISIBLE);
    bringToFront();
  }

  public void hide() {
    mView.setVisibility(View.GONE);
  }

  @Override
  public void onTouch(MotionEvent event) {
    hide();
  }

  @Override
  public void onScaleChanged(float scale, float previous) {
    hide();
  }

}
