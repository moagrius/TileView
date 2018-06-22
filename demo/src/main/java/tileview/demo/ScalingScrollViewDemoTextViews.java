package tileview.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.LinearLayout;

import com.qozix.widget.ScalingScrollView;

/**
 * @author Mike Dunn, 2/3/18.
 */

public class ScalingScrollViewDemoTextViews extends Activity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_scalingscrollview_textviews);
    ScalingScrollView scalingScrollView = findViewById(R.id.scalingscrollview);
    scalingScrollView.setShouldVisuallyScaleContents(true);
    LinearLayout linearLayout = findViewById(R.id.linearlayout);
    for (int i = 0; i < 100; i++) {
      LinearLayout row = new LinearLayout(this);
      Helpers.populateLinearLayout(row, 100);
      linearLayout.addView(row);
    }
  }

}
