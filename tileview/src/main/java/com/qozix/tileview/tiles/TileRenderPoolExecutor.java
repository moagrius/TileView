package com.qozix.tileview.tiles;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by bruno on 11/02/16.
 */
public class TileRenderPoolExecutor {
    // Sets the amount of time an idle thread will wait for a task before terminating
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;

    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAXIMUM_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private final ThreadPoolExecutor mExecutor;
    private final BlockingQueue<Runnable> mQueue;

    private static TileRenderPoolExecutor sInstance = null;

    static {
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        sInstance = new TileRenderPoolExecutor();
    }

    public TileRenderPoolExecutor() {
        mQueue = new LinkedBlockingDeque<>();
        mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mQueue);
    }

    public static TileRenderPoolExecutor getsInstance(){
        return sInstance;
    }

    public boolean isQueueEmpty(){
        synchronized (this){
            return mQueue.size() == 0;
        }
    }

    public void clearQueue(){
        synchronized (this){
            mQueue.clear();
        }
    }

    public void queue(TileCanvasViewGroup tileCanvasViewGroup, LinkedList<Tile> tiles){
        if(tiles!=null&&tiles.size()>0) {
            tileCanvasViewGroup.onRenderTaskPreExecute();
            int size = tiles.size();
            for(int i=0;i<size;i++){
                mExecutor.execute(new TileRenderRunnable(tileCanvasViewGroup, tiles.get(i), i == (size - 1)));
            }
        }
    }

    static class TileRenderRunnable implements Runnable{
        private final WeakReference<TileCanvasViewGroup> mTileCanvasViewGroup;
        private final WeakReference<Tile> mTile;
        private final boolean mIsLast;

        public TileRenderRunnable(TileCanvasViewGroup tileCanvasViewGroup, Tile tile, boolean last) {
            this.mTileCanvasViewGroup = new WeakReference<>(tileCanvasViewGroup);
            this.mTile = new WeakReference<>(tile);
            this.mIsLast = last;
        }

        @Override
        public void run() {
            // Moves the current Thread into the background
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

            TileCanvasViewGroup tileCanvasViewGroup = mTileCanvasViewGroup.get();
            Tile tile = mTile.get();

            if(!Thread.interrupted()&&tileCanvasViewGroup!=null&&!tileCanvasViewGroup.getRenderIsCancelled()&&tile!=null){
                tileCanvasViewGroup.generateTileBitmap(tile);

                //TODO check if thread was not interrupted again
                tileCanvasViewGroup.addTileToCurrentTileCanvasView(tile);

                if(mIsLast){
                    tileCanvasViewGroup.onRenderTaskPostExecute();
                }
            }
        }
    }
}
