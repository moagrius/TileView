package com.qozix.tileview.io;

import android.content.Context;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

public class StreamProviderHttp implements StreamProvider {

  @Override
  public InputStream getStream(int column, int row, Context context, Object data) throws Exception {
    String location = String.format(Locale.US, (String) data, column, row);
    URL url = new URL(location);
    URLConnection connection = url.openConnection();
    return connection.getInputStream();
  }

}
