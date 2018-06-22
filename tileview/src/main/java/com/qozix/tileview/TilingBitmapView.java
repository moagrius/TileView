package com.qozix.tileview;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

public class TilingBitmapView extends View {

  private Provider mProvider;
  private boolean mIsDirty;

  public TilingBitmapView(Provider provider) {
    super(provider.getContext());
    mProvider = provider;
  }

  public void setDirty() {
    if (mIsDirty) {
      return;
    }
    mIsDirty = true;
    postInvalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    canvas.save();
    canvas.scale(mProvider.getScale(), mProvider.getScale());
    mProvider.drawTiles(canvas);
    canvas.restore();
    mIsDirty = false;
  }

  public interface Provider {
    Context getContext();
    float getScale();
    void drawTiles(Canvas canvas);
  }

}
