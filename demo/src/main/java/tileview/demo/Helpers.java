package tileview.demo;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import java.util.Locale;

/**
 * @author Mike Dunn, 6/11/17.
 */

public class Helpers {

  public static void populateLinearLayout(LinearLayout linearLayout, int quantity) {
    populateLinearLayout(linearLayout, quantity, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT), "TextView #%1$d");
  }

  public static void populateLinearLayout(LinearLayout linearLayout, int quantity, LayoutParams lp, String text) {
    Context context = linearLayout.getContext();
    for (int i = 0; i < quantity; i++) {
      TextView textView = new TextView(context);
      textView.setText(String.format(Locale.US, text, i));
      textView.setTextSize(30);
      textView.setPadding(100, 100, 100, 100);
      textView.setBackgroundColor(0xFF383838);
      textView.setTextColor(0xFFD8D8D8);
      linearLayout.addView(textView, lp);
    }
  }

}
