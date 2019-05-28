package com.moagrius.tileview.plugins;

import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.moagrius.tileview.TileView;

public class InfoWindowPlugin extends FrameLayout implements TileView.Plugin, TileView.Listener, TileView.TouchListener {

  private View mView;

  public InfoWindowPlugin(View view) {
    super(view.getContext());
    mView = view;
    if (mView.getLayoutParams() == null) {
      LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
      mView.setLayoutParams(lp);
    }
    addView(mView);
    hide();
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
    setPosition(
        (int) (x + mView.getMeasuredWidth() * anchorX),
        (int) (y + mView.getMeasuredHeight() * anchorY));
    bringToFront();
  }

  public void hide() {
    setPosition(Integer.MIN_VALUE, Integer.MIN_VALUE);
  }

  private void setPosition(int x, int y) {
    LayoutParams lp = (LayoutParams) mView.getLayoutParams();
    if (lp == null) {
      lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
      mView.setLayoutParams(lp);
    }
    lp.leftMargin = x;
    lp.topMargin = y;
    mView.setLeft(x);
    mView.setTop(y);
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
