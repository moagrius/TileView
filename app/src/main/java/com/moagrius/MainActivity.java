package com.moagrius;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

  private static final int WRITE_REQUEST_CODE = 1;

  private Button buttonInternalStorage;
  private Button buttonExternalStorage;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    prepareButtonsToCopyFiles();
    findViewById(R.id.textview_demos_tileview_internal).setOnClickListener(this::showInternalStorageDemoOrWarning);
    findViewById(R.id.textview_demos_tileview_external).setOnClickListener(this::showExternalStorageDemoOrWarning);
    findViewById(R.id.textview_demos_tileview_simple).setOnClickListener(view -> startDemo(TileViewDemoSimple.class));
    findViewById(R.id.textview_demos_tileview_advanced).setOnClickListener(view -> startDemo(TileViewDemoAdvanced.class));

  }

  private void startDemo(Class<? extends Activity> activityClass) {
    Intent intent = new Intent(this, activityClass);
    startActivity(intent);
  }

  private void showInternalStorageDemoOrWarning(View view) {
    if (Helpers.getBooleanPreference(this, Helpers.INTERNAL_STORAGE_KEY)) {
      startDemo(TileViewDemoInternalStorage.class);
    } else {
      showToast("Copy tiles to internal storage using the buttons below first");
    }
  }

  private void showExternalStorageDemoOrWarning(View view) {
    if (Helpers.getBooleanPreference(this, Helpers.EXTERNAL_STORAGE_KEY)) {
      startDemo(TileViewDemoExternalStorage.class);
    } else {
      showToast("Copy tiles to external storage using the buttons below first");
    }
  }

  private void prepareButtonsToCopyFiles() {
    findViewById(R.id.button_copy_to_external).setOnClickListener(this::copyToExternalAsync);
    findViewById(R.id.button_copy_to_internal).setOnClickListener(this::copyToInternalAsync);
  }

  private void copyToExternalAsync(View view) {
    Log.d("TV", "copy to external async");
    if (Helpers.getBooleanPreference(this, Helpers.EXTERNAL_STORAGE_KEY)) {
      showWarning();
      return;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
      requestPermissions(permissions, WRITE_REQUEST_CODE);
    } else {
      new Thread(this::copyToExternalSync).start();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
      case WRITE_REQUEST_CODE:
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          new Thread(this::copyToExternalSync).start();
        }
        break;
    }
  }

  private void copyToInternalAsync(View view) {
    if (Helpers.getBooleanPreference(this, Helpers.INTERNAL_STORAGE_KEY)) {
      showWarning();
      return;
    }
    new Thread(this::copyToInternalSync).start();
  }

  private void copyToExternalSync() {
    Log.d("TV", "copy to external sync");
    try {
      Log.d("TV", "about to copy assets to external storage");
      Helpers.copyAssetTilesToExternalStorage(this);
    } catch (Exception e) {
      Log.d("TV", "there was an error copying files: " + e.getMessage());
      showToast("Error copying files to external storage: " + e.getMessage());
    }
    showToast("Files copied to external storage");
  }

  private void copyToInternalSync() {
    try {
      Helpers.copyAssetTilesToInternalStorage(this);
    } catch (Exception e) {
      showToast("Error copying files to internal storage: " + e.getMessage());
    }
    showToast("Files copied to internal storage");
  }

  private void showToast(String message) {
    runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
  }

  private void showWarning() {
    showToast("You have already copied these files. If this is not correct, please reinstall the demo");
  }


}
