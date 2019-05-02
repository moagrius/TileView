package com.moagrius;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {

  private static final int WRITE_REQUEST_CODE = 1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    findViewById(R.id.button_copy_to_internal).setOnClickListener(view -> copyAssetTilesToDirectoryAsync(Helpers.INTERNAL_STORAGE_KEY, Environment.getExternalStorageDirectory()));
    findViewById(R.id.button_copy_to_external).setOnClickListener(view -> copyAssetTilesToDirectoryAsync(Helpers.EXTERNAL_STORAGE_KEY, getFilesDir()));
    findViewById(R.id.textview_demos_tileview_internal).setOnClickListener(view -> showStorageDemoOrWarning(Helpers.INTERNAL_STORAGE_KEY, TileViewDemoInternalStorage.class));
    findViewById(R.id.textview_demos_tileview_external).setOnClickListener(view -> showStorageDemoOrWarning(Helpers.EXTERNAL_STORAGE_KEY, TileViewDemoExternalStorage.class));
    findViewById(R.id.textview_demos_tileview_simple).setOnClickListener(view -> startDemo(TileViewDemoSimple.class));
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

  private void showStorageDemoOrWarning(String key, Class<? extends Activity> activityClass) {
    if (Helpers.getBooleanPreference(this, key)) {
      startDemo(activityClass);
    } else {
      showToast("Copy tiles to the appropriate storage directory using the buttons below first");
    }
  }

  private void copyAssetTilesToDirectoryAsync(String preference, File directory) {
    Log.d("TV", "copy to external async");
    if (Helpers.getBooleanPreference(this, preference)) {
      showWarning(directory);
      return;
    }
    new Thread(() -> copyAssetTilesToDirectorySync(directory)).start();
  }

  private void copyAssetTilesToDirectorySync(File directoryy) {
    try {
      Helpers.copyAssetTilesToDirectory(this, directoryy);
    } catch (Exception e) {
      showToast("Error copying files to storage: " + e.getMessage());
    }
    showToast("Files copied to storage");
  }

  private void showToast(String message) {
    runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
  }

  private void showWarning(File directory) {
    new AlertDialog.Builder(this)
        .setTitle("Warning")
        .setMessage("It looks like you've already copied this files.  Click IGNORE to proceed anyway")
        .setPositiveButton(R.string.label_ignore, (d, i) -> new Thread(() -> copyAssetTilesToDirectorySync(directory)).start())
        .setNegativeButton(R.string.label_cancel, null)
        .create()
        .show();
  }


}
