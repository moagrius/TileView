package tileview.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.qozix.tileview.graphics.BitmapProvider;
import com.qozix.tileview.tiles.Tile;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * Created by bruno on 11/02/16.
 */
public class BitmapHttpProvider implements BitmapProvider {
    private static final BitmapFactory.Options OPTIONS = new BitmapFactory.Options();
    public static final String TAG = BitmapHttpProvider.class.getSimpleName();

    static {
        OPTIONS.inPreferredConfig = Bitmap.Config.RGB_565;
    }

    @Override
    public Bitmap getBitmap(Tile tile, Context context) {
        Object data = tile.getData();
        if(data instanceof String){
            String fileName = String.format(Locale.getDefault(), (String)data, tile.getColumn(), tile.getRow());
            try {
                URL url = new URL(fileName);
                try {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    InputStream input = connection.getInputStream();
                    if ( input != null ) {
                        try {
                            return BitmapFactory.decodeStream( input, null, OPTIONS );
                        } catch ( OutOfMemoryError oom ) {
                            // oom - you can try sleeping (this method won't be called in the UI thread) or try again (or give up)
                        } catch ( Exception e ) {
                            // unknown error decoding bitmap
                        }
                    }
                } catch ( IOException e ) {
                    Log.e(TAG, "IOException", e);
                    // io error/
                }
            } catch ( MalformedURLException e1 ) {
                Log.e(TAG, "MalformedURLException", e1);
                // bad url
            }
        }
        return null;
    }
}
