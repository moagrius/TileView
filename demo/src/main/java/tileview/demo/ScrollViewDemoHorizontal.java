package tileview.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

/**
 * @author Mike Dunn, 6/11/17.
 */

public class ScrollViewDemoHorizontal extends Activity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_scrollview_horizontal);
    LinearLayout linearLayout = findViewById(R.id.linearlayout);
    Helpers.populateLinearLayout(
        linearLayout, 100,
        new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT),
        "This TextView (number %1$d)\n" +
            "is longer than other so that\n" +
            "the horizontal ScrollView shows\n" +
            "a more pager-like display");
  }
}
