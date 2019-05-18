package com.moagrius;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import com.moagrius.helpers.FileCopier;
import com.moagrius.helpers.Helpers;

import java.io.File;

public class MainActivity extends Activity implements FileCopier.Listener {

  private static final int WRITE_REQUEST_CODE = 1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    findViewById(R.id.textview_demos_tileview_internal).setOnClickListener(view -> showStorageDemoOrWarning(Helpers.INTERNAL_STORAGE_KEY, getFilesDir(), TileViewDemoInternalStorage.class));
    findViewById(R.id.textview_demos_tileview_external).setOnClickListener(view -> showStorageDemoOrWarning(Helpers.EXTERNAL_STORAGE_KEY, Environment.getExternalStorageDirectory(), TileViewDemoExternalStorage.class));
    //findViewById(R.id.textview_demos_tileview_remote).setOnClickListener(view -> startDemo(TileViewDemoHttp.class));
    findViewById(R.id.textview_demos_tileview_assets).setOnClickListener(view -> startDemo(TileViewDemoAssets.class));
    findViewById(R.id.textview_demos_tileview_advanced).setOnClickListener(view -> startDemo(TileViewDemoAdvanced.class));
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
      requestPermissions(permissions, WRITE_REQUEST_CODE);
    }
  }

  private void startDemo(Class<? extends Activity> activityClass) {
    Intent intent = new Intent(this, activityClass);
    startActivity(intent);
  }

  private void showStorageDemoOrWarning(String preference, File directory, Class<? extends Activity> activityClass) {
    if (Helpers.getBooleanPreference(this, preference)) {
      startDemo(activityClass);
    } else {
      new AlertDialog.Builder(this)
          .setTitle("Warning")
          .setMessage("It looks like you haven't copied those files from the assets directory yet.  Do so now?")
          .setPositiveButton(R.string.label_accept, (d, i) -> copyAssetTilesToDirectory(preference, directory))
          .setNegativeButton(R.string.label_cancel, null)
          .create()
          .show();
    }
  }

  private void copyAssetTilesToDirectory(String preference, File directory) {
    if (Helpers.getBooleanPreference(this, preference)) {
      showWarning(preference, directory);
      return;
    }
    new FileCopier(directory, preference, this).copyFilesAsync(this);
  }

  private void showToast(String message) {
    runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
  }

  private void showWarning(String preference, File directory) {
    new AlertDialog.Builder(this)
        .setTitle("Warning")
        .setMessage("It looks like you've already copied this files.  Click IGNORE to proceed anyway")
        .setPositiveButton(R.string.label_ignore, (d, i) -> copyAssetTilesToDirectory(preference, directory))
        .setNegativeButton(R.string.label_cancel, null)
        .create()
        .show();
  }

  @Override
  public void onProgress(File file) {
    showToast("Copied " + file);
  }

  @Override
  public void onComplete(File directory) {
    showToast("Copied all files to " + directory);
  }

  @Override
  public void onError(Throwable throwable) {
    showToast("There was an error during the copy operation: " + throwable.getMessage());
  }

}
