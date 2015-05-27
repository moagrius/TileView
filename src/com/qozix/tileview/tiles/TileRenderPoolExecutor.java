package com.qozix.tileview.tiles;

import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by bruno on 26/05/15.
 */
public class TileRenderPoolExecutor {
    private static final String TAG = TileRenderPoolExecutor.class.getSimpleName();

    // Sets the amount of time an idle thread will wait for a task before terminating
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;

    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAXIMUM_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private final ThreadPoolExecutor mDownloadThreadPool;
    private final BlockingQueue<Runnable> mDownloadWorkQueue;

    private static TileRenderPoolExecutor sInstance = null;
    private boolean mCancelled;

    static {
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        sInstance = new TileRenderPoolExecutor();
    }

    private TileRenderPoolExecutor(){
        mDownloadWorkQueue = new LinkedBlockingQueue<>();
        mDownloadThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mDownloadWorkQueue);
    }

    public static TileRenderPoolExecutor getInstance(){
        return sInstance;
    }

    public void queue(TileManager tileManager, LinkedList<Tile> toRender){
        tileManager.onRenderTaskPreExecute();
        mCancelled = false;
        for(int i=0;i<toRender.size();i++){
            mDownloadThreadPool.execute(new TileDownloadRunner(tileManager, toRender.get(i), i==toRender.size()-1));

        }
    }

    public boolean isQueueEmpty(){
        synchronized (this) {
            return mDownloadWorkQueue.size() == 0;
        }
    }

    public void clearQueue(){
        synchronized (this) {
            mDownloadWorkQueue.clear();
            mCancelled = true;
        }
    }

    public boolean isCancelled(){
        return mCancelled;
    }
}
