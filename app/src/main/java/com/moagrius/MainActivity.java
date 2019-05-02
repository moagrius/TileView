package com.moagrius;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

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
      startDemo(TileViewDemoInternalStorage.class);
    } else {
      showToast("Copy tiles to external storage using the buttons below first");
    }
  }

  private void prepareButtonsToCopyFiles() {
    findViewById(R.id.button_copy_to_external).setOnClickListener(this::copyToExternalAsync);
    findViewById(R.id.button_copy_to_internal).setOnClickListener(this::copyToInternalAsync);
  }

  private void copyToExternalAsync(View view) {
    if (Helpers.getBooleanPreference(this, Helpers.EXTERNAL_STORAGE_KEY)) {
      showWarning();
      return;
    }
    new Thread(this::copyToExternalSync).start();
  }

  private void copyToInternalAsync(View view) {
    if (Helpers.getBooleanPreference(this, Helpers.INTERNAL_STORAGE_KEY)) {
      showWarning();
      return;
    }
    new Thread(this::copyToInternalSync).start();
  }

  private void copyToExternalSync() {
    try {
      Helpers.copyAssetTilesToExternalStorage(this);
    } catch (Exception e) {
      showToast("Error copying files: " + e.getMessage());
    }
    showToast("Files copied to external storage");
  }

  private void copyToInternalSync() {
    try {
      Helpers.copyAssetTilesToExternalStorage(this);
    } catch (Exception e) {
      showToast("Error copying files: " + e.getMessage());
    }
    showToast("Files copied to internal storage");
  }

  private void showToast(String message) {
    runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
  }

  private void showWarning() {
    showToast("You have already copied these files");
  }


}
