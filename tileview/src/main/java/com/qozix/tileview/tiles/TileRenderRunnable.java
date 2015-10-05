package com.qozix.tileview.tiles;

import java.lang.ref.WeakReference;

public class TileRenderRunnable implements Runnable {

  private WeakReference<Tile> mTileWeakReference;
  private WeakReference<TileManager> mTileManagerWeakReference;

  public TileRenderRunnable( TileManager tileManager, Tile tile ) {
    mTileWeakReference = new WeakReference<Tile>( tile );
    mTileManagerWeakReference = new WeakReference<TileManager>( tileManager );
  }

  @Override
  public void run() {
    decode();
  }

  private void decode(){
    TileManager tileManager = mTileManagerWeakReference.get();
    if (tileManager != null) {
      if(!tileManager.getRenderIsCancelled()){
        Tile tile = mTileWeakReference.get();
        if (tile != null) {
          tileManager.decodeIndividualTile( tile );
          render();
        }
      }

    }
  }

  private void render(){
    TileManager tileManager = mTileManagerWeakReference.get();
    if (tileManager != null) {
      if(!tileManager.getRenderIsCancelled()) {
        Tile tile = mTileWeakReference.get();
        if (tile != null) {
          tileManager.renderIndividualTile( tile );
        }
      }
    }
  }

}
