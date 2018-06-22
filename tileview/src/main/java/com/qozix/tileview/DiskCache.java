package com.qozix.tileview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DiskCache implements TileView.BitmapCache {

  private static final String DIRECTORY_NAME = "tileview-cache";
  private static final int IO_BUFFER_SIZE = 8 * 1024;

  private DiskLruCache mDiskCache;

  public DiskCache(Context context, int size) throws IOException {
    File directory = new File(context.getCacheDir(), DIRECTORY_NAME);
    mDiskCache = DiskLruCache.open(directory, 1, 1, size);
  }

  @Override
  public Bitmap put(String key, Bitmap data) {
    if (contains(key)) {
      return data;
    }
    DiskLruCache.Editor editor = null;
    try {
      editor = mDiskCache.edit(key);
      if (editor != null) {
        if (writeBitmapToCache(data, editor)) {
          mDiskCache.flush();
          editor.commit();
        } else {
          editor.abort();
        }
      }
    } catch (IOException e) {
      try {
        if (editor != null) {
          editor.abort();
        }
      } catch (IOException ignored) {
        //
      }
    }
    return data;
  }

  @Override
  public Bitmap get(String key) {
    DiskLruCache.Snapshot snapshot = null;
    try {
      snapshot = mDiskCache.get(key);
      if (snapshot == null) {
        return null;
      }
      InputStream inputStream = snapshot.getInputStream(0);
      if (inputStream != null) {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, IO_BUFFER_SIZE);
        return BitmapFactory.decodeStream(bufferedInputStream);
      }
    } catch (IOException e) {
      // no op
    } finally {
      if (snapshot != null) {
        snapshot.close();
      }
    }
    return null;
  }

  @Override
  public Bitmap remove(String key) {
    try {
      mDiskCache.remove(key);
    } catch (IOException e) {
      // no op
    }
    return null;
  }

  private boolean writeBitmapToCache(Bitmap bitmap, DiskLruCache.Editor editor) {
    OutputStream outputStream = null;
    try {
      outputStream = editor.newOutputStream(0);
      outputStream = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);
      return bitmap.compress(CompressFormat.PNG, 0, outputStream);
    } catch (Exception e) {
      // no op
    } finally {
      try {
        if (outputStream != null) {
          outputStream.close();
        }
      } catch (IOException e) {
        // no op
      }
    }
    return false;
  }

  private boolean contains(String key) {
    boolean contained = false;
    DiskLruCache.Snapshot snapshot = null;
    try {
      snapshot = mDiskCache.get(key);
      contained = snapshot != null;
    } catch (IOException e) {
      // no op
    } finally {
      if (snapshot != null) {
        snapshot.close();
      }
    }
    return contained;
  }

  public void clear() {
    try {
      mDiskCache.delete();
    } catch (IOException e) {
      // no op
    }
  }

}
