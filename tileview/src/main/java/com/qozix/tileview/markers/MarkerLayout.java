package com.qozix.tileview.markers;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;

import com.qozix.tileview.widgets.TranslationLayout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

public class MarkerLayout extends TranslationLayout {

  private HashMap<View, Rect> mMarkerMap = new HashMap<View, Rect>();
  private HashSet<MarkerEventListener> mListeners = new HashSet<MarkerEventListener>();

  public MarkerLayout( Context context ) {
    super( context );
  }

  public View addMarker( View view, int x, int y ) {
    return addMarker( view, x, y, null, null );
  }

  public View addMarker( View view, int x, int y, Float aX, Float aY ) {
    LayoutParams layoutParams = new LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, x, y, aX, aY );
    return addMarker( view, layoutParams );
  }

  public View addMarker( View view, LayoutParams params ) {
    addView( view, params );
    mMarkerMap.put( view, new Rect() );
    requestLayout();
    return view;
  }

  public void moveMarker( View view, int x, int y ) {
    moveMarker( view, x, y, null, null );
  }

  public void moveMarker( View view, int x, int y, Float aX, Float aY ) {
    LayoutParams layoutParams = new LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, x, y, aX, aY );
    moveMarker( view, layoutParams );
  }

  public void moveMarker( View view, LayoutParams params ) {
    if( mMarkerMap.containsKey( view ) ) {
      view.setLayoutParams( params );
      requestLayout();
    }
  }

  public void removeMarker( View view ) {
    removeView( view );
    mMarkerMap.remove( view );
  }

  public void addMarkerEventListener( MarkerEventListener listener ) {
    mListeners.add( listener );
  }

  public void removeMarkerEventListener( MarkerEventListener listener ) {
    mListeners.remove( listener );
  }

  private View getViewFromTap( int x, int y ) {
    Iterator<Entry<View, Rect>> iterator = mMarkerMap.entrySet().iterator();
    while( iterator.hasNext() ) {
      Entry<View, Rect> pairs = iterator.next();
      Rect rect = pairs.getValue();
      if( rect.contains( x, y ) ) {
        return pairs.getKey();
      }
    }
    return null;
  }

  public void processHit( Point point ) {
    if( mListeners.isEmpty() ) {
      return;
    }
    View view = getViewFromTap( point.x, point.y );
    if( view != null ) {
      for( MarkerEventListener listener : mListeners ) {
        listener.onMarkerTap( view, point.x, point.y );
      }
    }
  }

  // TODO: zindex should be y based, so marker bottoms don't show
  @Override
  protected void onLayout( boolean changed, int l, int t, int r, int b ) {
    super.onLayout( changed, l, t, r, b );  // TODO: don't run through everything twice
    for( int i = getChildCount() - 1; i >= 0; i-- ) {
      View child = getChildAt( i );
      if( child.getVisibility() != GONE ) {
        LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
        // get sizes
        int w = child.getMeasuredWidth();
        int h = child.getMeasuredHeight();
        // get offset position
        int scaledX = (int) (0.5 + (layoutParams.x * mScale));
        int scaledY = (int) (0.5 + (layoutParams.y * mScale));
        // user child's layout params anchor position if set, otherwise default to anchor position of layout
        float aX = (layoutParams.anchorX == null) ? mAnchorX : layoutParams.anchorX;
        float aY = (layoutParams.anchorY == null) ? mAnchorY : layoutParams.anchorY;
        // apply anchor offset to position
        int x = scaledX + (int) (w * aX);
        int y = scaledY + (int) (h * aY);
        // get and set the rect for the child
        Rect rect = mMarkerMap.get( child );
        if( rect != null ) {
          rect.set( x, y, x + w, y + h );
        }
      }
    }
  }

}
