package com.qozix.tileview.io;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class StreamProviderAssets implements StreamProvider {
  @Override
  public InputStream getStream(int column, int row, Context context, Object data) throws IOException {
    String file = String.format(Locale.US, (String) data, column, row);
    return context.getAssets().open(file);
  }
}
