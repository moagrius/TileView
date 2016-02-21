package tileview.demo.provider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.qozix.tileview.graphics.BitmapProvider;
import com.qozix.tileview.tiles.Tile;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

public class BitmapHttpProvider implements BitmapProvider {
  private static final BitmapFactory.Options OPTIONS = new BitmapFactory.Options();
  public static final String TAG = BitmapHttpProvider.class.getSimpleName();

  static {
    OPTIONS.inPreferredConfig = Bitmap.Config.RGB_565;
  }

  @Override
  public Bitmap getBitmap( Tile tile, Context context ) {
    Object data = tile.getData();
    Bitmap bitmap = null;
    if( data instanceof String ) {
      String fileName = String.format( Locale.getDefault(), (String) data, tile.getColumn(), tile.getRow() );
      try {
        URL url = new URL( fileName );
        try {
          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
          InputStream input = connection.getInputStream();
          if( input != null ) {
            try {
              bitmap = BitmapFactory.decodeStream( input, null, OPTIONS );
            }  catch( Exception e ) {
              Log.d( "DEBUG", "getBitmap" );
            } catch( Throwable t ) {
              Log.d( "DEBUG", "ERROR!!!" );
            }
          }
        } catch( InterruptedIOException e ) {
          Thread.currentThread().interrupt();
          Log.d( "DEBUG", "thread interrupted, should not return the incomplete bitmap: " + tile.getColumn() + ", " + tile.getRow() );
        } catch( Exception e ) {
          Log.d( "DEBUG", "BitmapHttpProvider.getBitmap, input maybe?" );
        }
      } catch( MalformedURLException e1 ) {
        Log.d( "DEBUG", "MalformedURLException " + fileName, e1 );
      }
    }
    return bitmap;
  }
}
