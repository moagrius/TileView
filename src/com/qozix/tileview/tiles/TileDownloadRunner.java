package com.qozix.tileview.tiles;

import java.lang.ref.WeakReference;

public class TileDownloadRunner implements Runnable {
	private final WeakReference<TileManager> reference;
	private final WeakReference<Tile> tile;
	private final boolean last;

	public TileDownloadRunner(TileManager tileManager, Tile tile, boolean isLast) {
		this.tile = new WeakReference<>(tile);
		this.reference = new WeakReference<>(tileManager);
		last = isLast;
	}

	@Override
	public void run() {
		TileManager tileManager = reference.get();
		Tile tile = this.tile.get();
		if(tileManager !=null){
			if(!tileManager.getRenderIsCancelled()&&tile!=null){
				tileManager.decodeIndividualTile(tile);
				tileManager = reference.get();
				if ( !tileManager.getRenderIsCancelled() && !TileRenderPoolExecutor.getInstance().isCancelled()) {
					tileManager.renderIndividualTile( tile );
				}
				if(last){
					tileManager.onRenderTaskPostExecute();
				}
			}
		}
	}
}
