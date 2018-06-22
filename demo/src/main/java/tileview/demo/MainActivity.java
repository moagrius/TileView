package tileview.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    findViewById(R.id.textview_demos_scrollview_vertical).setOnClickListener(view -> startDemo(ScrollViewDemoVertical.class));
    findViewById(R.id.textview_demos_scrollview_horizontal).setOnClickListener(view -> startDemo(ScrollViewDemoHorizontal.class));
    findViewById(R.id.textview_demos_scrollview_universal).setOnClickListener(view -> startDemo(ScrollViewDemoUniversal.class));
    findViewById(R.id.textview_demos_scalingscrollview_textviews).setOnClickListener(view -> startDemo(ScalingScrollViewDemoTextViews.class));
    findViewById(R.id.textview_demos_scalingscrollview_tiger).setOnClickListener(view -> startDemo(ScalingScrollViewDemoTiger.class));
    findViewById(R.id.textview_demos_tileview_advanced).setOnClickListener(view -> startDemo(TileViewDemoAdvanced.class));
    findViewById(R.id.textview_demos_tileview_simple).setOnClickListener(view -> startDemo(TileViewDemoSimple.class));
  }

  private void startDemo(Class<? extends Activity> activityClass) {
    Intent intent = new Intent(this, activityClass);
    startActivity(intent);
  }

}
