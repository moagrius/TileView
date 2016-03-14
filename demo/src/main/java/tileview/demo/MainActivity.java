package tileview.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

	@Override
	protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate( savedInstanceState );
		setContentView( R.layout.main );

		HashMap<Integer, Class<?>> implementations = new HashMap<>();
		implementations.put( R.id.show_image, LargeImageTileViewActivity.class );
		implementations.put( R.id.show_plans, BuildingPlansTileViewActivity.class );
		implementations.put( R.id.show_fiction, FictionalMapTileViewActivity.class );
		implementations.put( R.id.show_map, RealMapTileViewActivity.class );
		implementations.put( R.id.show_internet, RealMapInternetTileViewActivity.class );

		for (Map.Entry<Integer, Class<?>> entry : implementations.entrySet()) {
			TextView label = (TextView) findViewById( entry.getKey() );
			label.setTag( entry.getValue() );
			label.setOnClickListener( labelClickListener );
		}

	}

	private View.OnClickListener labelClickListener = new View.OnClickListener() {
		@Override
		public void onClick( View v ) {
			Class<?> activity = (Class<?>) v.getTag();
			Intent intent = new Intent( MainActivity.this, activity );
			startActivity( intent );
		}
	};
}
