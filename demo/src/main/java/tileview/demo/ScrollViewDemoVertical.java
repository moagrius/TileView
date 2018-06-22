package tileview.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.LinearLayout;

/**
 * @author Mike Dunn, 6/11/17.
 */

public class ScrollViewDemoVertical extends Activity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_scrollview_vertical);
    LinearLayout linearLayout = findViewById(R.id.linearlayout);
    Helpers.populateLinearLayout(linearLayout, 20);
  }
}
