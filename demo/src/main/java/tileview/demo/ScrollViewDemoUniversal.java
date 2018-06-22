package tileview.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.LinearLayout;

/**
 * @author Mike Dunn, 6/11/17.
 */

public class ScrollViewDemoUniversal extends Activity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_scrollview_universal);
    LinearLayout linearLayout = findViewById(R.id.linearlayout);
    for (int i = 0; i < 20; i++) {
      LinearLayout row = new LinearLayout(this);
      Helpers.populateLinearLayout(row, 100);
      linearLayout.addView(row);
    }
  }
}
