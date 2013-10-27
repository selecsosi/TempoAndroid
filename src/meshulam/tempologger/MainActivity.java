package meshulam.tempologger;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {
	private final static String TAG = "MainActivity";
	
	private TextView view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		view = new TextView(this);
		view.setTextSize(24);
		view.setGravity(Gravity.CENTER);
		view.setText(R.string.default_text);
		setContentView(view);
    }
    
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    }
    
    private void startService() {
    	ComponentName name = startService(new Intent(this, MonitorService.class));
    	if (name != null) {
    		Log.i(TAG, "Starting background service: "+name.flattenToString());
    	} else {
    		Log.i(TAG, "Starting background service (failed)");
    	}
    	
    }
    
    private void stopService() {
    	boolean success = stopService(new Intent(this, MonitorService.class));
    	Log.i(TAG, "Stopping background service success="+success);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.start_service:
        	startService();
        	return true;
        case R.id.stop_service:
        	stopService();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
