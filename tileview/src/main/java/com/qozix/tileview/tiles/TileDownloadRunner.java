package com.qozix.tileview.tiles;

import java.lang.ref.WeakReference;

public class TileDownloadRunner implements Runnable {
	private final WeakReference<TileManager> mTileManagerWeakReference;
	private final WeakReference<Tile> mTileWeakReference;
	private final boolean last;

	public TileDownloadRunner( TileManager tileManager, Tile tile, boolean isLast ) {
		mTileWeakReference = new WeakReference<>( tile );
		mTileManagerWeakReference = new WeakReference<>( tileManager );
		last = isLast;
	}

	@Override
	public void run() {
		TileManager tileManager = mTileManagerWeakReference.get();
		if (tileManager != null) {
			if (!tileManager.getRenderIsCancelled()) {
				Tile tile = mTileWeakReference.get();
				if(tile != null){
					tileManager.decodeIndividualTile( tile );
					tileManager = mTileManagerWeakReference.get();
					if (!tileManager.getRenderIsCancelled() && !TileRenderPoolExecutor.getInstance().isCancelled()) {
						tileManager.renderIndividualTile( tile );
					}
					if (last) {
						tileManager.onRenderTaskPostExecute();
					}
				}
			}

		}
	}
}
