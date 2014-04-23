package com.tempodb.tempologger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * Service which should be running all the time. Starts a timer which checks once a minute if any tasks are due.
 * The real work is done in the MonitorTask.
 *
 */
public class MonitorService extends Service {
	private final static String TAG = "MonitorService";
	
	private final static int COLLECTION_INTERVAL_S = 60;
	
	// Which sensors to try to collect data from
	private final static int[] sensorList = { 	Sensor.TYPE_LIGHT,
												Sensor.TYPE_AMBIENT_TEMPERATURE,
												Sensor.TYPE_PRESSURE,
												Sensor.TYPE_MAGNETIC_FIELD,
                                                Sensor.TYPE_RELATIVE_HUMIDITY,
                                                Sensor.TYPE_ACCELEROMETER,
                                                };
	
	private Set<Sensor> availableSensors = new HashSet<Sensor>();
	private SensorManager sensorManager;
	private Timer serviceTimer;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (serviceTimer != null) 
			serviceTimer.cancel();
		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		Sensor s;
		for (int type : sensorList) {
			s = sensorManager.getDefaultSensor(type);
			if (s != null) availableSensors.add(s);
		}
		
		serviceTimer = new Timer("Tempo-ServiceTimer");
		serviceTimer.scheduleAtFixedRate(new CollectionTimer(), 0, COLLECTION_INTERVAL_S*1000);
		
		Log.i(TAG, "onStartCommand");
		Toast.makeText(this, "Starting TempoLogger service", Toast.LENGTH_SHORT).show();
		
		return START_STICKY;
	}
	
	class CollectionTimer extends TimerTask {
		@Override
		public void run() {
			Log.i(TAG, "Time to take some sensor readings");
			SensorAggregator listener = new SensorAggregator();
			
			for (Sensor sensor : availableSensors) {
				sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
			}
		}
	}
	
	// Listens for sensor updates scheduled by the CollectionTimer and unsubscribes after receiving each type of reading.
	class SensorAggregator implements SensorEventListener {
		Map<Sensor, Float> data = new HashMap<Sensor, Float>();
		
		@Override
		public void onSensorChanged(SensorEvent event) {
			float value;
			
			switch (event.sensor.getType()) {
			case Sensor.TYPE_MAGNETIC_FIELD:		// 3D vector measurements-- convert XYZ to magnitude 
			case Sensor.TYPE_ACCELEROMETER:
			case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
			case Sensor.TYPE_GRAVITY:
            case Sensor.TYPE_GYROSCOPE:
			case Sensor.TYPE_LINEAR_ACCELERATION:
                double[] filterPoints = new double[3];
                double alpha = 0.8;
                filterPoints[0] = alpha * filterPoints[0] + (1 - alpha) * event.values[0];
                filterPoints[1] = alpha * filterPoints[1] + (1 - alpha) * event.values[1];
                filterPoints[2] = alpha * filterPoints[2] + (1 - alpha) * event.values[2];
				value = (float) Math.sqrt( filterPoints[0]*filterPoints[0] +
                        filterPoints[1]*filterPoints[1] +
                        filterPoints[2]*filterPoints[2] );
				break;
			default:	// Scalar measurements
				value = event.values[0];
			}
			Log.i(TAG, "Got value "+value+" for sensor "+event.sensor.getName());
			data.put(event.sensor, value);
			
			if (data.size() >= availableSensors.size()) {	// Got all the readings we're expecting
				sensorManager.unregisterListener(this);
				Thread t = new Thread(new Runnable() {
							public void run() {
								TempoAdapter.getInstance().putData(data);					
							}
						});
				t.start();
			}
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// do nothing
		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy - stopping service");
		Toast.makeText(this, "Stopping TempoLogger service", Toast.LENGTH_SHORT).show();
		if (serviceTimer != null)
			serviceTimer.cancel();
	}

}
