package tileview.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.qozix.widget.ScalingScrollView;


/**
 * @author Mike Dunn, 2/3/18.
 */

public class ScalingScrollViewDemoTiger extends Activity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_scalingscrollview_tiger);
    ScalingScrollView scalingScrollView = findViewById(R.id.scalingscrollview);
    scalingScrollView.setScaleLimits(0, 10);
    scalingScrollView.setShouldVisuallyScaleContents(true);
  }
}
