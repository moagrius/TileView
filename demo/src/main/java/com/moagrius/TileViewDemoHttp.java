package com.moagrius;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.moagrius.tileview.Tile;
import com.moagrius.tileview.TileView;
import com.moagrius.tileview.io.StreamProviderHttp;
import com.moagrius.tileview.plugins.LowFidelityBackgroundPlugin;

public class TileViewDemoHttp extends TileViewDemoActivity implements TileView.TileDecodeErrorListener {

  private TileView mTileView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_tileview);
    frameToCenterOnReady();
    mTileView = findViewById(R.id.tileview);
    mTileView.setTileDecodeErrorListener(this);
    new TileView.Builder(mTileView)
        .setSize(16384, 13056)
        .setStreamProvider(new StreamProviderHttp())
        .setDiskCachePolicy(TileView.DiskCachePolicy.CACHE_ALL)
        .installPlugin(new LowFidelityBackgroundPlugin(getBackgroundBitmap()))
        .defineZoomLevel("https://storage.googleapis.com/moagrius_tiles/tiles/phi-1000000-%1$d_%2$d.jpg")
        //.defineZoomLevel(1, "https://storage.googleapis.com/tileview_tiles/tiles/phi-500000-%1$d_%2$d.jpg")
        //.defineZoomLevel(2, "https://storage.googleapis.com/tileview_tiles/tiles/phi-250000-%1$d_%2$d.jpg")
        .build();
  }

  @Override
  public void onTileDecodeError(Tile tile, Exception e) {
    mTileView.retryTileDecode(tile);
  }

  public Bitmap getBackgroundBitmap() {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = Bitmap.Config.RGB_565;
    return BitmapFactory.decodeResource(getResources(), R.drawable.downsample, options);
  }

}
