package com.tempodb.tempologger;

import java.util.*;
import java.util.Map.Entry;

import com.tempodb.*;
import org.joda.time.DateTime;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.util.Log;

public class TempoAdapter {
	private final static String TAG = "TempoAdapter";
	
	private final static String API_KEY = "b51670cec3734c2289fed511defe405e";
	private final static String API_SECRET = "3f53e62c5465442cb6ac11e3356da3ff";
    private final static String DATABASE_NAME = "b51670cec3734c2289fed511defe405e";

	private static Map<Integer, String> supportedDataSeries = new HashMap<Integer, String>();
	
	static {
		// Initialize the mappings from Android sensors to Tempo data series keys.
		supportedDataSeries.put(Sensor.TYPE_LIGHT, "LightSensorData");
		supportedDataSeries.put(Sensor.TYPE_LINEAR_ACCELERATION, "AccelerationSensorData");
		supportedDataSeries.put(Sensor.TYPE_AMBIENT_TEMPERATURE, "AmbientTemperatureData");
		supportedDataSeries.put(Sensor.TYPE_MAGNETIC_FIELD, "MagneticFieldData");
		supportedDataSeries.put(Sensor.TYPE_PRESSURE, "PressureData");
        supportedDataSeries.put(Sensor.TYPE_ACCELEROMETER, "AccelerometerData");
        supportedDataSeries.put(Sensor.TYPE_RELATIVE_HUMIDITY, "RelativeHumidityData");
        supportedDataSeries.put(Sensor.TYPE_GYROSCOPE, "GyroscopeData");
	}

    private Map<String, Series> cachedDataSeries = new HashMap<String, Series>();

	private static TempoAdapter instance;
	
	private Client client;
	private Filter seriesFilter;
	
	private TempoAdapter() {
        Credentials myCredentials = new Credentials(API_KEY, API_SECRET);
        //Construct a client that will be used to make the requests
        client = new ClientBuilder()
                .credentials(myCredentials)
                .database(new Database(DATABASE_NAME))
                .build();
        seriesFilter = new Filter();
        for(Entry<Integer, String> entry : supportedDataSeries.entrySet()) {
            cachedDataSeries.put(entry.getValue(), null);
            seriesFilter.addKey(entry.getValue());
        }
	}
	
	public static TempoAdapter getInstance() {
		if (instance == null) 
			instance = new TempoAdapter();
		return instance;
	}

    private void populateSensorSeriesCacheIfNecessary() {
        if(cachedDataSeries.containsValue(null)) {
            //Try to update the sensor cache with series that we already have created
            populateSensorSeriesCache(getSensorSeriesCursor(this.seriesFilter));

            //Iterate through the cachedDataSeries set to see what values are still null
            //That means we need to create those series on the server
            for(Entry<String, Series> entry : cachedDataSeries.entrySet()) {
                if(entry.getValue() == null) {
                    entry.setValue(createSeries(entry.getKey()));
                }
            }
        }
    }

    public Series createSeries(String key) {
        Series s = new Series(key);
        Result<Series> result = client.createSeries(s);
        if(result.getState() == State.SUCCESS) {
            return result.getValue();
        } else {
            return null;
        }
    }

    public void populateSensorSeriesCache(Cursor<Series> seriesCursor) {
        for(Series s : seriesCursor) {
            this.cachedDataSeries.put(s.getKey(), s);
        }
    }

    public Cursor<Series> getSensorSeriesCursor(Filter filter) {
        return client.getSeries(filter);
    }
	
	public void putData(Map<Sensor, Float> data) {
        populateSensorSeriesCacheIfNecessary();

		List<DataPoint> points = new ArrayList<DataPoint>();
        DateTime sensorTimestamp = DateTime.now();

        WriteRequest request = new WriteRequest();

		for (Entry<Sensor, Float> entry : data.entrySet()) {
			int sensorType = entry.getKey().getType();
            Series s = cachedDataSeries.get(supportedDataSeries.get(sensorType));
            request.add(s, new DataPoint(sensorTimestamp, entry.getValue()));
		}
		
		try {
            Result<Void> result = client.writeDataPoints(request);
		} catch (Exception e) {
			Log.e(TAG, "Exception when writing data points!");
			e.printStackTrace();
		}
	}
}
