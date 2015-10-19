package com.qozix.tileview.tiles;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.LinkedList;

class TileRenderTask extends AsyncTask<Void, Tile, Void> {

	private final WeakReference<TileCanvasViewGroup> mTileManagerWeakReference;

	// package level access
	TileRenderTask( TileCanvasViewGroup tileCanvasViewGroup ) {
		super();
		mTileManagerWeakReference = new WeakReference<TileCanvasViewGroup>( tileCanvasViewGroup );
	}
	
	@Override
	protected void onPreExecute() {
		final TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
		if ( tileCanvasViewGroup != null ) {
			tileCanvasViewGroup.onRenderTaskPreExecute();
		}		
	}

	/**
	 * As of 10/03/15, lint is _incorrectly_ indicating that we can't access member
	 * variables from a worker thread (this thread).
   * https://code.google.com/p/android/issues/detail?id=175397
   * Until this is corrected, use @SuppressWarnings
	 * @param params noop
	 * @return null
	 */
  @SuppressWarnings("all")
	@Override
	protected Void doInBackground( Void... params ) {
		// have we been stopped or dereffed?
		TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
		// if not go ahead, but check again in each iteration
		if ( tileCanvasViewGroup != null ) {
			// avoid concurrent modification exceptions by duplicating
			LinkedList<Tile> renderList = tileCanvasViewGroup.getRenderList();
			// start rendering, checking each iteration if we need to break out
			for ( Tile tile : renderList ) {
				// check again if we've been stopped or gc'ed
				tileCanvasViewGroup = mTileManagerWeakReference.get();
				if ( tileCanvasViewGroup == null ) {
					return null;
				}
				// quit if we've been forcibly stopped
				if ( tileCanvasViewGroup.getRenderIsCancelled() ) {
					return null;
				}
				// quit if task has been cancelled or replaced
				if ( isCancelled() ) {
					return null;
				}
				// once the bitmap is decoded, the heavy lift is done
				tileCanvasViewGroup.decodeIndividualTile( tile );
				// pass it to the UI thread for insertion into the view tree
				publishProgress( tile );
			}
			
		}		
		return null;
	}

	@Override
	protected void onProgressUpdate( Tile... params ) {
		// have we been stopped or dereffed?
		TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
		// if not go ahead but check other cancel states
		if ( tileCanvasViewGroup != null ) {
			// quit if it's been force-stopped
			if ( tileCanvasViewGroup.getRenderIsCancelled() ) {
				return;
			}
			// quit if it's been stopped or replaced by a new task
			if ( isCancelled() ) {
				return;
			}
			// tile should already have bitmap decoded
			Tile tile = params[0];
			// add the bitmap to it's view, add the view to the current detail level layout
			tileCanvasViewGroup.renderIndividualTile( tile );
		}
		
	}

	@Override
	protected void onPostExecute( Void param ) {
		// have we been stopped or dereffed?
		TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
		// if not go ahead but check other cancel states
		if ( tileCanvasViewGroup != null ) {
			tileCanvasViewGroup.onRenderTaskPostExecute();
		}
	}

	@Override
	protected void onCancelled() {
		// have we been stopped or dereffed?
		TileCanvasViewGroup tileCanvasViewGroup = mTileManagerWeakReference.get();
		// if not go ahead but check other cancel states
		if ( tileCanvasViewGroup != null ) {
			tileCanvasViewGroup.onRenderTaskCancelled();
		}
	}

}