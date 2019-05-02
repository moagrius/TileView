package com.moagrius.tileview.io;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;

public class StreamProviderFiles implements StreamProvider {

  @Override
  public InputStream getStream(int column, int row, Context context, Object data) throws Exception {
    String path = String.format(Locale.US, (String) data, column, row);
    File file = new File(path);
    InputStream stream = new FileInputStream(file);
    return new BufferedInputStream(stream);
  }

}
