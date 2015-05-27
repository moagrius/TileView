package com.qozix.tileview.tiles;

import java.lang.ref.WeakReference;

/**
 * Created by bruno on 26/05/15.
 */
public class TileDownloadRunner implements Runnable {
    private final WeakReference<TileManager> mReference;
    private final WeakReference<Tile> mTile;
    private final boolean mLast;

    public TileDownloadRunner(TileManager tileManager, Tile tile, boolean isLast) {
        this.mTile = new WeakReference<>(tile);
        this.mReference = new WeakReference<>(tileManager);
        mLast = isLast;
    }

    @Override
    public void run() {
        //android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);//TODO play around with priority

        TileManager tileManager = mReference.get();
        Tile tile = mTile.get();
        if(tileManager !=null){
            if(!tileManager.getRenderIsCancelled()&&tile!=null){
                tileManager.decodeIndividualTile(tile);
                tileManager = mReference.get();
                if ( !tileManager.getRenderIsCancelled() && !TileRenderPoolExecutor.getInstance().isCancelled()) {
                    tileManager.renderIndividualTile( tile );
                }
                if(mLast){
                    tileManager.onRenderTaskPostExecute();
                }
            }
        }
    }
}
