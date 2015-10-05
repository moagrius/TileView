package com.qozix.tileview.tiles;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class TileRenderPoolExecutor {

    private static class SingletonHolder {
        private static TileRenderPoolExecutor INSTANCE;
    }

    private final ThreadPoolExecutor downloadThreadPool;

    private boolean cancelled;

    private TileRenderPoolExecutor(){
        downloadThreadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    public static TileRenderPoolExecutor getInstance(){
        return SingletonHolder.INSTANCE;
    }

    public void queue(TileManager tileManager, LinkedList<Tile> toRender){
        tileManager.onRenderTaskPreExecute();
        cancelled = false;
        for(int i=0;i<toRender.size();i++){
            downloadThreadPool.execute(new TileDownloadRunner(tileManager, toRender.get(i), i == toRender.size() - 1));

        }
    }

    public void cancel(){
        cancelled = true;
        downloadThreadPool.shutdownNow();
    }

    public boolean isCancelled(){
        return cancelled;
    }
}
