package meshulam.tempologger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.util.Log;

import com.tempodb.client.Client;
import com.tempodb.client.ClientBuilder;
import com.tempodb.models.BulkDataSet;
import com.tempodb.models.BulkKeyPoint;
import com.tempodb.models.BulkPoint;
import com.tempodb.models.DataSet;
import com.tempodb.models.Filter;

public class TempoAdapter {
	private final static String TAG = "TempoAdapter";
	
	private final static String API_KEY = "34b138cb3817486b87e585838312f5f0";
	private final static String API_SECRET = "b11f2ed6238e43f4b6aecab21c208628";

	private static Map<Integer, String> supportedDataSeries = new HashMap<Integer, String>();
	
	static {
		// Initialize the mappings from Android sensors to Tempo data series keys.
		supportedDataSeries.put(Sensor.TYPE_LIGHT, "LightSensorData");
		supportedDataSeries.put(Sensor.TYPE_LINEAR_ACCELERATION, "AccelerationSensorData");
		supportedDataSeries.put(Sensor.TYPE_AMBIENT_TEMPERATURE, "AmbientTemperatureData");
		supportedDataSeries.put(Sensor.TYPE_MAGNETIC_FIELD, "MagneticFieldData");
		supportedDataSeries.put(Sensor.TYPE_PRESSURE, "PressureData");
	}
	
	private static TempoAdapter instance;
	
	private Client client; 
	//private Filter seriesFilter;
	
	private TempoAdapter() {
		client = new ClientBuilder()
			.key(API_KEY)
			.secret(API_SECRET)
			.build();
	}
	
	public static TempoAdapter getInstance() {
		if (instance == null) 
			instance = new TempoAdapter();
		return instance;
	}
	
	public void putData(Map<Sensor, Float> data) {
		List<BulkPoint> points = new ArrayList<BulkPoint>();
		
		for (Entry<Sensor, Float> entry : data.entrySet()) {
			int sensorType = entry.getKey().getType();
		
			if (supportedDataSeries.containsKey(sensorType)) {
				points.add(new BulkKeyPoint(supportedDataSeries.get(sensorType), entry.getValue()));
			}
		}
		
		try {
			client.bulkWrite(new BulkDataSet(
									DateTime.now(),
									points));
		} catch (Exception e) {
			Log.e(TAG, "Exception when writing data points!");
			e.printStackTrace();
		}
	}
	

}
