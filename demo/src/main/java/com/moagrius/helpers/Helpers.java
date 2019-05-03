package com.moagrius.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Helpers {

  public static final String EXTERNAL_STORAGE_KEY = "external";
  public static final String INTERNAL_STORAGE_KEY = "internal";

  private static final String PREFS_FILE_NAME = "preferences";

  public static void copyStreams(InputStream inputStream, OutputStream outputStream) throws IOException {
    try {
      int data = inputStream.read();
      while (data != -1) {
        outputStream.write(data);
        data = inputStream.read();
      }
    } finally {
      inputStream.close();
      outputStream.close();
    }
  }

  public static void saveBooleanPreference(Context context, String key, boolean value) {
    SharedPreferences preferences = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean(key, value);
    editor.apply();
  }

  public static boolean getBooleanPreference(Context context, String key) {
    return context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getBoolean(key, false);
  }

}
