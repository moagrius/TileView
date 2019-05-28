package com.moagrius.tileview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.moagrius.tileview.io.StreamProvider;
import com.moagrius.tileview.io.StreamProviderAssets;
import com.moagrius.utils.Maths;
import com.moagrius.widget.ScalingScrollView;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TileView extends ScalingScrollView implements
    Handler.Callback,
    ScalingScrollView.ScaleChangedListener,
    Tile.DrawingView,
    Tile.Listener,
    TilingBitmapView.Provider {

  public static void printStackTrace() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    int count = stackTrace.length;
    String[] messages = new String[count - 2];
    for (int i = 2; i < count; i++) {
      StackTraceElement stackTraceElement = stackTrace[i];
      messages[i - 2] = stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName();
    }
    Log.d("TV", "STACKTRACE: " + Arrays.toString(messages));
  }

  // constants
  private static final int RENDER_THROTTLE_ID = 0;
  private static final int RENDER_THROTTLE_INTERVAL = 15;
  private static final short DEFAULT_TILE_SIZE = 256;
  private static final int READY_RETRY_DELAY = 250;

  // variables (settable)
  private int mZoom = 0;
  private int mImageSample = 1; // sample will always be one unless we don't have a defined detail level, then its 1 shl for every zoom level from the last defined detail
  private int mTileSize = DEFAULT_TILE_SIZE;
  private boolean mIsPrepared;
  private boolean mIsLaidOut;
  private boolean mHasRunOnReady;
  private Detail mCurrentDetail;

  private Set<Listener> mListeners = new LinkedHashSet<>();
  private Set<ReadyListener> mReadyListeners = new LinkedHashSet<>();
  private Set<TouchListener> mTouchListeners = new LinkedHashSet<>();
  private Set<CanvasDecorator> mCanvasDecorators = new LinkedHashSet<>();
  private TileDecodeErrorListener mTileDecodeErrorListener;

  // variables (from build or attach)
  private FixedSizeViewGroup mContainer;
  private TilingBitmapView mTilingBitmapView;
  private BitmapCache mDiskCache;
  private BitmapCache mMemoryCache;
  private BitmapPool mBitmapPool;
  private StreamProvider mStreamProvider;
  private Builder mBuilder;
  private Bitmap.Config mBitmapConfig = Bitmap.Config.RGB_565;
  private DiskCachePolicy mDiskCachePolicy = DiskCachePolicy.CACHE_PATCHES;

  // final
  private final Grid mGrid = new Grid();
  private final DetailList mDetailList = new DetailList();
  private final Map<Class<? extends Plugin>, Plugin> mPlugins = new HashMap<>();

  // we keep our tiles in Sets
  // that means we're ensured uniqueness (so we don't have to think about if a tile is already scheduled or not)
  // and O(1) contains, as well as no penalty with foreach loops (excluding the allocation of the iterator)
  // we use the specific LinkedHashSet implementation to take advantage of potentially faster iteration on optimized VMS
  // at the potential expense of more space required
  // https://lemire.me/blog/2018/03/13/iterating-over-hash-sets-quickly-in-java/
  // we'll use enhanced for loops without testing empty as well https://stackoverflow.com/a/20898524/6585616
  private final Set<Tile> mNewlyVisibleTiles = new LinkedHashSet<>();
  private final Set<Tile> mTilesVisibleInViewport = new LinkedHashSet<>();
  private final Set<Tile> mPreviouslyDrawnTiles = new LinkedHashSet<>();

  private final Rect mViewport = new Rect();
  private final Rect mScaledViewport = new Rect();  // really just a buffer for unfilled region
  private final Region mUnfilledRegion = new Region();

  private final TilePool mTilePool = new TilePool(this::createTile);
  private final TileRenderExecutor mExecutor = new TileRenderExecutor();
  private final ThreadPoolExecutor mDiskCacheExecutor = new ThreadPoolExecutor(0, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
  private final Handler mRenderThrottle = new Handler(this);
  private final Handler mReadyHandler = new Handler();

  public TileView(Context context) {
    this(context, null);
  }

  public TileView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TileView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setClipChildren(false);
    setScaleChangedListener(this);
    // as a ScrollView subclass, we can only use one child.
    // this could be the TilingBitmapView if we didn't allow plugins
    // use FixedSizeViewGroup as a simple, optimized layering scheme
    // by calling it during construction, no other views will be allowed
    // (unless the user hacks intended behavior by removing all views or by index)
    mContainer = new FixedSizeViewGroup(context);
    // we'll draw bitmaps to this view
    mTilingBitmapView = new TilingBitmapView(this);
    mContainer.addView(mTilingBitmapView);
    // call the full signature, otherwise one overloaded signature may call another
    // e.g., ViewGroup.addView(child) will call ViewGroup.addView(child, -1, ...)
    // which will end up placing the child in the TileView rather than the container
    super.addView(mContainer, -1, generateDefaultLayoutParams());
  }

  @Override
  public void addView(View child) {
    mContainer.addView(child);
  }

  @Override
  public void addView(View child, int index) {
    mContainer.addView(child, index);
  }

  @Override
  public void addView(View child, ViewGroup.LayoutParams params) {
    mContainer.addView(child, params);
  }

  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    mContainer.addView(child, index, params);
  }

  @Override
  public void removeAllViews() {
    mContainer.removeAllViews();
  }

  @Override
  public void removeViewAt(int index) {
    mContainer.removeViewAt(index);
  }

  @Override
  public void removeViews(int start, int count) {
    mContainer.removeViews(start, count);
  }

  @Override
  protected void restoreInstanceState(Parcelable state) {
    mScrollScaleState = (ScrollScaleState) state;
    int x = mScrollScaleState.scrollPositionX - getWidth() / 2;
    int y = mScrollScaleState.scrollPositionY - getHeight() / 2;
    Log.d("TV", "restoreInstanceState" +
        ", x=" + mScrollScaleState.scrollPositionX +
        ", y=" + mScrollScaleState.scrollPositionY +
        ", scale=" + mScrollScaleState.scale);
    scrollTo(x, y);
    setScale(mScrollScaleState.scale);
  }

  @Override
  protected Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
        ScrollScaleState scrollScaleState = new ScrollScaleState(superState);
        scrollScaleState.scale = getScale();
        scrollScaleState.scrollPositionY = getScrollY() + getWidth() / 2;
        scrollScaleState.scrollPositionX = getScrollX() + getHeight() / 2;
        Log.d("TV", "saveInstanceState" +
            ", x=" + scrollScaleState.scrollPositionX +
            ", y=" + scrollScaleState.scrollPositionY +
            ", scale=" + scrollScaleState.scale);
        return scrollScaleState;
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    mIsPrepared = false;
    mIsLaidOut = false;
    Log.d("TV", "context=" + getContext() + ", isFinishing? " + ((Activity) getContext()).isFinishing());
  }

  private ScrollScaleState mScrollScaleState;
  private Float mPendingScale;
  private Integer mPendingX;
  private Integer mPendingY;

  @Override
  public void setScale(float scale) {
    if (isReady()) {
      Log.d("TV", "setScale, " + scale + ", isReady, run now");
      super.setScale(scale);
    } else {
      Log.d("TV", "setScale, " + scale + ", NOT ready, set pending");
      mPendingScale = scale;
    }
  }

  @Override
  public void scrollTo(int x, int y) {
    if (isReady()) {
      Log.d("TV", "scrollTo, " + x + ", " + y + ", isReady, run now");
      super.scrollTo(x, y);
    } else {
      Log.d("TV", "scrollTo, " + x + ", " + y + ", NOT ready, set pending");
      mPendingX = x;
      mPendingY = y;
    }
    printStackTrace();
  }

  // public

  public int getZoom() {
    return mZoom;
  }

  public int getScaledWidth() {
    return (int) (mContainer.getFixedWidth() * getScale());
  }

  public int getScaledHeight() {
    return (int) (mContainer.getFixedHeight() * getScale());
  }

  public int getUnscaledContentWidth() {
    return mContainer.getFixedWidth();
  }

  public int getUnscaledContentHeight() {
    return mContainer.getFixedHeight();
  }

  public void setTileDecodeErrorListener(TileDecodeErrorListener listener) {
    mTileDecodeErrorListener = listener;
  }

  @Override
  public int getContentWidth() {
    return getScaledWidth();
  }

  @Override
  public int getContentHeight() {
    return getScaledHeight();
  }

  public boolean addListener(Listener listener) {
    return mListeners.add(listener);
  }

  public boolean removeListener(Listener listener) {
    return mListeners.remove(listener);
  }

  public boolean addReadyListener(ReadyListener readyListener) {
    if (isReady()) {
      readyListener.onReady(this);
      return false;
    }
    return mReadyListeners.add(readyListener);
  }

  public boolean removeReadyListener(ReadyListener readyListener) {
    return mReadyListeners.remove(readyListener);
  }

  public boolean addCanvasDecorator(CanvasDecorator decorator) {
    return mCanvasDecorators.add(decorator);
  }

  public boolean removeCanvasDecorator(CanvasDecorator decorator) {
    return mCanvasDecorators.remove(decorator);
  }

  public boolean addTouchListener(TouchListener touchListener) {
    return mTouchListeners.add(touchListener);
  }

  public boolean removeTouchListener(TouchListener touchListener) {
    return mTouchListeners.remove(touchListener);
  }

  public ViewGroup getContainer() {
    return mContainer;
  }

  @SuppressWarnings("unchecked")
  public <T extends Plugin> T getPlugin(Class<T> clazz) {
    return (T) mPlugins.get(clazz);
  }

  private void defineZoomLevel(int zoom, Object data) {
    mDetailList.set(zoom, new Detail(zoom, data));
    determineCurrentDetail();
  }

  private void centerVisibleChildren() {
    final int scaledWidth = getScaledWidth();
    final int scaledHeight = getScaledHeight();
    final int offsetX = scaledWidth >= getWidth() ? 0 : getWidth() / 2 - scaledWidth / 2;
    final int offsetY = scaledHeight >= getHeight() ? 0 : getHeight() / 2 - scaledHeight / 2;
    mContainer.setLeft(offsetX);
    mContainer.setTop(offsetY);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    //super.onLayout(changed, left, top, right, bottom);
    if (getChildCount() < 1) {
      return;
    }
    final View child = getChildAt(0);
    final int width = child.getMeasuredWidth();
    final int height = child.getMeasuredHeight();
    child.layout(0, 0, width, height);
    mIsLaidOut = true;
    Log.d("TV", "about to call attemptOnReady from onLayout");
    final boolean runningInitialization = attemptOnReady();
    if (!runningInitialization) {
      updateViewportAndComputeTilesThrottled();
    }
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    boolean result = super.onInterceptTouchEvent(event);
    if (!mTouchListeners.isEmpty()) {
      for (TouchListener touchListener : mTouchListeners) {
        touchListener.onTouch(event);
      }
    }
    return result;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean result = super.onTouchEvent(event);
    if (!mTouchListeners.isEmpty()) {
      for (TouchListener touchListener : mTouchListeners) {
        touchListener.onTouch(event);
      }
    }
    return result;
  }

  @Override
  protected void onScrollChanged(int x, int y, int previousX, int previousY) {
    super.onScrollChanged(x, y, previousX, previousY);
    updateViewportAndComputeTilesThrottled();
    for (Listener listener : mListeners) {
      listener.onScrollChanged(x, y);
    }
  }


  @Override
  public void onScaleChanged(ScalingScrollView scalingScrollView, float currentScale, float previousScale) {
    centerVisibleChildren();
    scrollTo(getScrollX(), getScrollY());
    for (Listener listener : mListeners) {
      listener.onScaleChanged(currentScale, previousScale);
    }
    int previousZoom = mZoom;
    mZoom = Detail.getZoomFromPercent(currentScale);
    if (mZoom < 0) {
      mZoom = 0;
    }
    boolean zoomChanged = mZoom != previousZoom;
    if (zoomChanged) {
      mPreviouslyDrawnTiles.clear();
      for (Tile tile : mTilesVisibleInViewport) {
        if (tile.getState() == Tile.State.DECODED) {
          mPreviouslyDrawnTiles.add(tile);
        }
      }
      mTilesVisibleInViewport.clear();
      determineCurrentDetail();
    }
    updateViewportAndComputeTilesThrottled();
    // if this is setDirty or postInvalidate, things get wonky
    mTilingBitmapView.invalidate();
    // if we call this in onZoomChanged, we might reference outdated values for viewport and tile sets
    if (zoomChanged) {
      for (Listener listener : mListeners) {
        listener.onZoomChanged(mZoom, previousZoom);
      }
    }
  }

  private void determineCurrentDetail() {
    // if zoom from scale is greater than the number of defined detail levels, we definitely don't have it
    // since it's not an exact match, we need to patch together bitmaps from the last known zoom level
    // so if we have a detail level defined for zoom level 1 (sample 2) but are on zoom level 2 (sample 4) we want an actual sample of 2
    // similarly if we have definition for sample zoom 1 / sample 2 and are on zoom 3 / sample 8, we want actual sample of 4
    // this is also the case for the third block, below.
    if (mZoom >= mDetailList.size()) {
      mCurrentDetail = mDetailList.getHighestDefined();
      int zoomDelta = mZoom - mCurrentDetail.getZoom();
      mImageSample = 1 << zoomDelta;
      return;
    }
    if (mZoom < 0) {
      mZoom = 0;
    }
    // best case, it's an exact match, use that and set sample to 1
    Detail exactMatch = mDetailList.get(mZoom);
    if (exactMatch != null) {
      mCurrentDetail = exactMatch;
      mImageSample = 1;
      return;
    }
    // it's not bigger than what we have defined, but we don't have an exact match, start at the requested zoom and work back
    // toward 0 (full size) until we find any defined detail level
    for (int i = mZoom - 1; i >= 0; i--) {
      Detail current = mDetailList.get(i);
      if (current != null) {  // if it's defined
        mCurrentDetail = current;
        int zoomDelta = mZoom - mCurrentDetail.getZoom();
        mImageSample = 1 << zoomDelta;
        return;
      }
    }
  }

  private void establishDirtyRegion() {
    mUnfilledRegion.set(mScaledViewport);
    // then punch holes in it for every decoded current tile
    // when drawing previous tiles, if there's no intersection with an unfilled area, it can be safely discarded
    // otherwise we should draw the previous tile
    for (Tile tile : mTilesVisibleInViewport) {
      if (tile.getState() == Tile.State.DECODED) {
        mUnfilledRegion.op(tile.getDrawingRect(), Region.Op.DIFFERENCE);
      }
    }
  }

  private void drawPreviousTiles(Canvas canvas) {
    establishDirtyRegion();
    if (mUnfilledRegion.isEmpty()) {
      return;
    }
    Iterator<Tile> iterator = mPreviouslyDrawnTiles.iterator();
    while (iterator.hasNext()) {
      Tile tile = iterator.next();
      Rect rect = tile.getDrawingRect();
      // if no part of the rect is in the unfilled area, we don't need it
      // use quickReject instead of quickContains because the latter does not work on complex Regions
      // https://developer.android.com/reference/android/graphics/Region.html#quickContains(android.graphics.Rect)
      if (mUnfilledRegion.quickReject(rect)) {
        tile.destroy();
        iterator.remove();
      } else {
        tile.draw(canvas);
      }
    }
  }

  private void drawCurrentTiles(Canvas canvas) {
    for (Tile tile : mTilesVisibleInViewport) {
      tile.draw(canvas);
    }
  }

  private void drawInterceptors(Canvas canvas) {
    if (!mCanvasDecorators.isEmpty()) {
      for (CanvasDecorator canvasDecorator : mCanvasDecorators) {
        canvasDecorator.decorate(canvas);
      }
    }
  }

  @Override
  public void drawTiles(Canvas canvas) {
    drawPreviousTiles(canvas);
    drawCurrentTiles(canvas);
    drawInterceptors(canvas);
  }

  @Override
  public void setDirty() {
    mTilingBitmapView.setDirty();
  }

  // Implementing Handler.Callback handleMessage to react to throttled requests to start a render op
  @Override
  public boolean handleMessage(Message message) {
    updateViewportAndComputeTiles();
    return true;
  }

  private void updateViewportAndComputeTiles() {
    if (isReady()) {
      updateViewport();
      computeAndRenderTilesInViewport();
    }
  }

  private void updateViewportAndComputeTilesThrottled() {
    if (!mRenderThrottle.hasMessages(RENDER_THROTTLE_ID)) {
      mRenderThrottle.sendEmptyMessageDelayed(RENDER_THROTTLE_ID, RENDER_THROTTLE_INTERVAL);
    }
  }

  private void updateViewport() {
    mViewport.left = getScrollX();
    mViewport.top = getScrollY();
    mViewport.right = mViewport.left + getMeasuredWidth();
    mViewport.bottom = mViewport.top + getMeasuredHeight();
    updateScaledViewport();
  }

  private void updateScaledViewport() {
    // set unfilled to entire viewport, virtualized to scale
    float scale = getScale();
    mScaledViewport.set(
        (int) (mViewport.left / scale),
        (int) (mViewport.top / scale),
        (int) (mViewport.right / scale),
        (int) (mViewport.bottom / scale)
    );
  }

  public void populateTileGridFromViewport() {
    float tileSize = mTileSize * getScale() * mCurrentDetail.getSample();
    mGrid.rows.start = Maths.roundDownWithStep(mViewport.top / tileSize, mImageSample);
    mGrid.rows.end = Maths.roundUpWithStep(mViewport.bottom / tileSize, mImageSample);
    mGrid.columns.start = Maths.roundDownWithStep(mViewport.left / tileSize, mImageSample);
    mGrid.columns.end = Maths.roundUpWithStep(mViewport.right / tileSize, mImageSample);
  }

  public Tile createTile() {
    return new Tile(mTileSize, mBitmapConfig, this, this, mExecutor, mDiskCacheExecutor, mStreamProvider, mMemoryCache, mDiskCache, mBitmapPool, mDiskCachePolicy);
  }

  private void computeAndRenderTilesInViewport() {
    // determine which tiles should be showing.  use sample size for patching very small tiles together
    mNewlyVisibleTiles.clear();
    populateTileGridFromViewport();
    for (int row = mGrid.rows.start; row < mGrid.rows.end; row += mImageSample) {
      for (int column = mGrid.columns.start; column < mGrid.columns.end; column += mImageSample) {
        Tile tile = mTilePool.get();
        tile.setColumn(column);
        tile.setRow(row);
        tile.setDetail(mCurrentDetail);
        tile.setImageSample(mImageSample);
        mNewlyVisibleTiles.add(tile);
      }
    }
    // update our sets to reflect the current state, schedule draws, and clean up
    Iterator<Tile> tilesVisibleInViewportIterator = mTilesVisibleInViewport.iterator();
    while (tilesVisibleInViewportIterator.hasNext()) {
      Tile tile = tilesVisibleInViewportIterator.next();
      // if a tile in the same zoom is not in the most recently computed grid, it's not longer "in viewport", remove it
      if (!mNewlyVisibleTiles.contains(tile)) {
        tile.destroy();
        tilesVisibleInViewportIterator.remove();
      }
    }
    // we just removed all tiles outside of the viewport, now add any new ones that are in the viewport that weren't there the last
    // time we performed this computation
    // we use add all instead of straight replacement because lets say tile(3:2) was being decoded - when tile(3:2) comes up in
    // mNewlyVisibleTiles, it won't be added to mTilesVisibleInViewport because Tile.equals will return true
    // if we just swapped out the set (mTilesVisibleInViewport = mNewlyVisibleTiles), all those tiles would lose their state
    boolean tilesWereAdded = mTilesVisibleInViewport.addAll(mNewlyVisibleTiles);
    if (tilesWereAdded) {
      mExecutor.queue(mTilesVisibleInViewport);
    }
  }

  private boolean isTileVisible(Tile tile) {
    return mTilesVisibleInViewport.contains(tile);
  }

  public void retryTileDecode(Tile tile, int attempts, boolean onlyIfVisible) {
    if (!onlyIfVisible || isTileVisible(tile)) {
      tile.retry(attempts);
    }
  }

  public void retryTileDecode(Tile tile) {
    retryTileDecode(tile, 1, true);
  }

  @Override
  public void onTileDestroyed(Tile tile) {
    mTilePool.put(tile);
  }

  @Override
  public void onTileDecodeError(Tile tile, Exception e) {
    if (mTileDecodeErrorListener != null) {
      mTileDecodeErrorListener.onTileDecodeError(tile, e);
    }
    Log.d("TileView", "tile decode error: " + e.getClass() + ", " + e.getMessage());
  }

  /**
   * Call this method when you're done with the TileView, perhaps in an `OnPause` event with an addiitonal check
   * for `isFinishing`.
   *
   * Note that the TileView will be unusable after this method is called.
   *
   * @param alsoCloseDiskCache Pass TRUE if you want to delete all tiles in the disk cache.  Depending on your
   *                           specific use case, this is probably not something you want to do.  If you're using
   *                           the same set of tiles, may as well keep them available on disk, especially when
   *                           pulling from a remote server.  However, if you tile source changes between launches,
   *                           then passing TRUE here might make sense.
   */
  public void destroy(boolean alsoCloseDiskCache) {
    mExecutor.shutdownNow();
    mDiskCacheExecutor.shutdownNow();
    mMemoryCache.clear();
    // note we are NOT clearing the diskcache by default this point, see the javadoc for that method for rational
    if (mDiskCache != null && alsoCloseDiskCache) {
      mDiskCacheExecutor.execute(mDiskCache::clear);
    }
    mTilePool.clear();
    mRenderThrottle.removeMessages(RENDER_THROTTLE_ID);
    mReadyHandler.removeCallbacksAndMessages(null);
  }

  public boolean isReady() {
    return mIsPrepared && mIsLaidOut;
  }

  public boolean hasRunOnReady() {
    return mHasRunOnReady;
  }

  public boolean isRunning() {
    return isReady() && hasRunOnReady();
  }

  private void prepare() {
    Log.d("TV", "prepare");
    if (mIsPrepared) {
      return;
    }
    if (mDetailList.isEmpty()) {
      throw new IllegalStateException("TileView requires at least one defined detail level");
    }
    if (!mContainer.hasValidDimensions()) {
      throw new IllegalStateException("TileView requires height and width be provided via Builder.setSize");
    }
    mIsPrepared = true;
    Log.d("TV", "about to call attemptOnReady from prepare");
    attemptOnReady();
  }

  /**
   *
   * @return True if the single ready pass executes, false otherwise (either because not ready, or already run)
   */
  private boolean attemptOnReady() {
    Log.d("TV", "attemptOnReady");
    if (isReady() && !mHasRunOnReady) {
      Log.d("TV", "isReady and hasn't yet run onReady");
      mHasRunOnReady = true;
      determineCurrentDetail();
      updateViewportAndComputeTiles();
      for (ReadyListener readyListener : mReadyListeners) {
        readyListener.onReady(this);
      }
      updatePendingValues();
      return true;
    }
    return false;
  }

  private void updatePendingValues() {
    if (mPendingX != null || mPendingY != null) {
      int x = 0;
      int y = 0;
      if (mPendingX != null) {
        x = mPendingX;
        mPendingX = null;
      }
      if (mPendingY != null) {
        y = mPendingY;
        mPendingY = null;
      }
      Log.d("TV", "scrolling to pending values, " + x + ", " + y);
      scrollTo(x, y);
    }
    if (mPendingScale != null) {
      float scale = mPendingScale;
      setScale(scale);
      mPendingScale = null;
    }
  }

  private static class Grid {
    Range rows = new Range();
    Range columns = new Range();
    private static class Range {
      int start;
      int end;
    }
  }

  public interface Plugin {
    void install(TileView tileView);
  }

  public interface BitmapCache {
    Bitmap get(String key);
    Bitmap put(String key, Bitmap value);
    Bitmap remove(String key);
    boolean has(String key);
    void clear();
  }

  public interface BitmapPool {
    Bitmap getBitmapForReuse(Tile tile);
  }

  public interface Listener {
    default void onZoomChanged(int zoom, int previous){}
    default void onScaleChanged(float scale, float previous){}
    default void onScrollChanged(int x, int y){}
  }

  public interface ReadyListener {
    void onReady(TileView tileView);
  }

  public interface TouchListener {
    void onTouch(MotionEvent event);
  }

  public interface CanvasDecorator {
    void decorate(Canvas canvas);
  }

  private static class FixedSizeViewGroup extends ViewGroup {

    private int mWidth;
    private int mHeight;

    public FixedSizeViewGroup(Context context) {
      super(context);
      setClipChildren(false);
    }

    public void setSize(int width, int height) {
      mWidth = width;
      mHeight = height;
      requestLayout();
    }

    public int getFixedWidth() {
      return mWidth;
    }

    public int getFixedHeight() {
      return mHeight;
    }

    public boolean hasValidDimensions() {
      return mWidth > 0 && mHeight > 0;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
      for (int i = 0; i < getChildCount(); i++) {
        View child = getChildAt(i);
        child.layout(0, 0, mWidth, mHeight);
      }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY);
      int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY);
      measureChildren(childWidthMeasureSpec, childHeightMeasureSpec);
      setMeasuredDimension(mWidth, mHeight);
    }

  }

  public static class Builder {

    private TileView mTileView;
    private StreamProvider mStreamProvider;
    private int mMemoryCacheSize = (int) ((Runtime.getRuntime().maxMemory() / 1024) / 4);
    private int mDiskCacheSize = 1024 * 100;
    private DiskCachePolicy mDiskCachePolicy;

    public Builder(TileView tileView) {
      mTileView = tileView;
      mTileView.mBuilder = this;
    }

    public Builder(Context context) {
      mTileView = new TileView(context);
      mTileView.mBuilder = this;
    }

    public Builder setSize(int width, int height) {
      mTileView.mContainer.setSize(width, height);
      return this;
    }

    public Builder defineZoomLevel(Object data) {
      return defineZoomLevel(0, data);
    }

    public Builder defineZoomLevel(int zoom, Object data) {
      mTileView.defineZoomLevel(zoom, data);
      return this;
    }

    public Builder addListener(TileView.Listener listener) {
      mTileView.addListener(listener);
      return this;
    }

    public Builder addReadyListener(TileView.ReadyListener readyListener) {
      mTileView.addReadyListener(readyListener);
      return this;
    }

    public Builder addTouchListener(TileView.TouchListener touchListener) {
      mTileView.addTouchListener(touchListener);
      return this;
    }

    public Builder addCanvasDecorator(TileView.CanvasDecorator decorator) {
      mTileView.addCanvasDecorator(decorator);
      return this;
    }

    public Builder setBitmapConfig(Bitmap.Config config) {
      mTileView.mBitmapConfig = config;
      return this;
    }

    public Builder setTileSize(int tileSize) {
      mTileView.mTileSize = tileSize;
      return this;
    }

    public Builder setDiskCachePolicy(DiskCachePolicy policy) {
      // save a member variable on the Builder so we can check it without having to hit the main thread for view access
      mDiskCachePolicy = mTileView.mDiskCachePolicy = policy;
      return this;
    }

    public Builder setMemoryCacheSize(int memoryCacheSize) {
      mMemoryCacheSize = memoryCacheSize;
      return this;
    }

    public Builder setDiskCacheSize(int diskCacheSize) {
      mDiskCacheSize = diskCacheSize;
      return this;
    }

    public Builder setStreamProvider(StreamProvider streamProvider) {
      mStreamProvider = streamProvider;
      return this;
    }

    public Builder installPlugin(Plugin plugin) {
      mTileView.mPlugins.put(plugin.getClass(), plugin);
      plugin.install(mTileView);
      return this;
    }

    public void build() {
      buildAsync();
    }

    private void buildAsync() {
      //new Thread(this::buildSync).start();
      buildSync();
    }

    private void buildSync() {
      Activity activity = (Activity) mTileView.getContext();
      if (activity == null) {
        Log.d("TileView", "could not not cast context to activity during preparation");
        return;
      }
      // if the user provided a custom provider, use that, otherwise default to assets
      if (mStreamProvider == null) {
        mStreamProvider = new StreamProviderAssets();
      }
      MemoryCache memoryCache = new MemoryCache(mMemoryCacheSize);
      DiskCache diskCache = getDiskCacheSafely(activity);
      activity.runOnUiThread(() -> {
        mTileView.mStreamProvider = mStreamProvider;
        // use memory cache instance for both memory cache and bitmap pool.
        // maybe allows these to be set in the future
        mTileView.mMemoryCache = memoryCache;
        mTileView.mBitmapPool = memoryCache;
        mTileView.mDiskCache = diskCache;
        mTileView.prepare();
      });
    }

    private DiskCache getDiskCacheSafely(Context context) {
      if (mDiskCachePolicy != DiskCachePolicy.CACHE_NONE && mDiskCacheSize > 0) {
        try {
          return new DiskCache(context, mDiskCacheSize);
        } catch (IOException e) {
          Log.d("TileView", "Unable to create DiskCache during preparation: " + e.getMessage());
        }
      }
      return null;
    }

  }

  public interface TileDecodeErrorListener {
    void onTileDecodeError(Tile tile, Exception e);
  }

  public enum DiskCachePolicy {
    CACHE_NONE, CACHE_PATCHES, CACHE_ALL
  }

}
