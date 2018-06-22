package com.qozix.tileview.io;

import android.content.Context;

import java.io.InputStream;
import java.util.Locale;

public class StreamProviderPicasso implements StreamProvider {

  @Override
  public InputStream getStream(int column, int row, Context context, Object data) throws Exception {
    String location = String.format(Locale.US, (String) data, column, row);
    // to avoid unnecessary dependencies, this returns null and the operative line has been commented out
    // in order to use it, remove the next line and uncomment the following line, after adding Picasso to your
    // project and importing it in this file
    return null;
    //return Picasso.with(context).load(formattedFileName).memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).get();
  }

}
