package com.moagrius.helpers;

import android.app.Activity;
import android.content.res.AssetManager;
import android.util.Log;

import com.moagrius.Helpers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileCopier {

  private File mDirectory;
  private String mPreferencesKey;
  private Listener mListener;

  public FileCopier(File directory, String preferencesKey, Listener listener) {
    mDirectory = directory;
    mPreferencesKey = preferencesKey;
    mListener = listener;
  }

  public void copyFiles(Activity activity) throws Exception {
    Log.d("TV", "about to copy asset tiles to " + mDirectory);
    Helpers.saveBooleanPreference(activity, mPreferencesKey, false);
    AssetManager assetManager = activity.getAssets();
    String[] assetPaths = assetManager.list("tiles");
    for (String assetPath : assetPaths) {
      InputStream assetStream = assetManager.open("tiles/" + assetPath);
      File dest = new File(mDirectory, assetPath);
      FileOutputStream outputStream = new FileOutputStream(dest);
      Helpers.copyStreams(assetStream, outputStream);
      Log.d("TV", assetPath + " copied to " + dest);
      activity.runOnUiThread(() -> mListener.onProgress(dest));
    }
    activity.runOnUiThread(() -> mListener.onComplete(mDirectory));
    Helpers.saveBooleanPreference(activity, mPreferencesKey, true);
    Log.d("TV", "done copying files");
  }

  public void copyFilesAsync(Activity activity) {
    new Thread(() -> {
      try {
        copyFiles(activity);
      } catch (Exception e) {
        mListener.onError(e);
      }
    }).start();
  }

  public interface Listener {
    void onProgress(File file);
    void onComplete(File directory);
    void onError(Throwable throwable);
  }

}
