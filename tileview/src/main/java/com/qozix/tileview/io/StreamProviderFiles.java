package com.qozix.tileview.io;

import android.content.Context;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;

public class StreamProviderFiles implements StreamProvider {

  @Override
  public InputStream getStream(int column, int row, Context context, Object data) throws Exception {
    String file = String.format(Locale.US, (String) data, column, row);
    return new FileInputStream(file);
  }

}
