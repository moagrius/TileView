package com.qozix.tileview.tiles;

import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TileRenderPoolExecutor {
    private static final String TAG = TileRenderPoolExecutor.class.getSimpleName();

    // Sets the amount of time an idle thread will wait for a task before terminating
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;

    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAXIMUM_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private final ThreadPoolExecutor downloadThreadPool;
    private final BlockingQueue<Runnable> downloadWorkQueue;

    private static TileRenderPoolExecutor instance = null;
    private boolean cancelled;

    static {
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        instance = new TileRenderPoolExecutor();
    }

    private TileRenderPoolExecutor(){
        downloadWorkQueue = new LinkedBlockingQueue<>();
        downloadThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, downloadWorkQueue);
    }

    public static TileRenderPoolExecutor getInstance(){
        return instance;
    }

    public void queue(TileManager tileManager, LinkedList<Tile> toRender){
        tileManager.onRenderTaskPreExecute();
        cancelled = false;
        for(int i=0;i<toRender.size();i++){
            downloadThreadPool.execute(new TileDownloadRunner(tileManager, toRender.get(i), i == toRender.size() - 1));

        }
    }

    public boolean isQueueEmpty(){
        synchronized (this) {
            return downloadWorkQueue.size() == 0;
        }
    }

    public void clearQueue(){
        synchronized (this) {
            downloadWorkQueue.clear();
            cancelled = true;
        }
    }

    public boolean isCancelled(){
        return cancelled;
    }
}
