package com.qozix.tileview;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TileRenderExecutor extends ThreadPoolExecutor {

  public TileRenderExecutor(int size) {
    super(size, size, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), task -> {
      Thread thread = new Thread(task);
      thread.setPriority(Thread.MIN_PRIORITY);
      return thread;
    });
  }

  public TileRenderExecutor() {
    this(Runtime.getRuntime().availableProcessors());
  }

  public void queue(Set<Tile> renderSet) {
    Iterator<Runnable> iterator = getQueue().iterator();
    while (iterator.hasNext()) {
      Tile tile = (Tile) iterator.next();
      if (!renderSet.contains(tile)) {
        tile.destroy(false);
        iterator.remove();
      }
    }
    for (Tile tile : renderSet) {
      if (isShutdownOrTerminating()) {
        return;
      }
      if (tile.getState() == Tile.State.IDLE) {
        execute(tile);
      }
    }
  }

  public void cancel() {
    for (Runnable runnable : getQueue()) {
      Tile tile = (Tile) runnable;
      tile.destroy();
    }
    getQueue().clear();
  }

  private boolean isShutdownOrTerminating() {
    return isShutdown() || isTerminating() || isTerminated();
  }

  @Override
  protected void afterExecute(Runnable runnable, Throwable throwable) {
    synchronized (this) {
      super.afterExecute(runnable, throwable);
      if (getQueue().size() == 0) {
        // TODO: notify something?
      }
    }
  }

}

