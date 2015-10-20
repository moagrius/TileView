package com.qozix.tileview.tiles;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.LinkedList;

class TileRenderTask extends AsyncTask<Void, Tile, Void> {

  private final WeakReference<TileCanvasViewGroup> mTileManagerWeakReference;

  TileRenderTask( TileCanvasViewGroup tileCanvasViewGroup ) {
    super();
    mTileManagerWeakReference = new WeakReference<TileCanvasViewGroup>( tileCanvasViewGroup );
  }

  @Override
  protected void onPreExecute() {
    final TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
    if( tileCanvasViewGroup != null ) {
      tileCanvasViewGroup.onRenderTaskPreExecute();
    }
  }

  /**
   * As of 10/03/15, lint is _incorrectly_ indicating that we can't access member
   * variables from a worker thread (this thread).
   * https://code.google.com/p/android/issues/detail?id=175397
   * Until this is corrected, use @SuppressWarnings
   *
   * @param params noop
   * @return null
   */
  @SuppressWarnings("all")
  @Override
  protected Void doInBackground( Void... params ) {
    TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
    if( tileCanvasViewGroup != null ) {
      LinkedList<Tile> renderList = tileCanvasViewGroup.getRenderList();
      for( Tile tile : renderList ) {
        if( !isCancelled() ) {
          tileCanvasViewGroup = mTileManagerWeakReference.get();
          if( tileCanvasViewGroup != null && !tileCanvasViewGroup.getRenderIsCancelled() ) {
            tileCanvasViewGroup.generateTileBitmap( tile );
            publishProgress( tile );
          }
        }
      }
    }
    return null;
  }

  @Override
  protected void onProgressUpdate( Tile... params ) {
    if( !isCancelled() ) {
      TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
      if( tileCanvasViewGroup != null && !tileCanvasViewGroup.getRenderIsCancelled() ) {
        Tile tile = params[0];
        tileCanvasViewGroup.addTileToCurrentTileCanvasView( tile );
      }
    }
  }

  @Override
  protected void onPostExecute( Void param ) {
    TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
    if( tileCanvasViewGroup != null ) {
      tileCanvasViewGroup.onRenderTaskPostExecute();
    }
  }

  @Override
  protected void onCancelled() {
    TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
    if( tileCanvasViewGroup != null ) {
      tileCanvasViewGroup.onRenderTaskCancelled();
    }
  }

}