package com.daranguiz.recolift;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;


/* This class implements the googleApiClient communication layer,
 * as well as the watch sensor collection.
 */
public class WatchDataLayerListenerService extends WearableListenerService
        implements SensorEventListener {
    public WatchDataLayerListenerService() {
    }

    private static final String TAG = "DataLayerListenerServ";
    public static String START_PATH = "config/start";
    public static String STOP_SERVICE_PATH = "config/stop_service";
    public static String STOP_COLLECTION_PATH = "config/stop_collection";
    public static final int MAX_EVENTS_IN_PACKET = 20;
    private GoogleApiClient mGoogleApiClient;

    /* Sensor Global Variables */
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mMagnet;
    private int mSensorCounter;
    private PutDataMapRequest sensorDataMap;
    // An aside, perhaps also incorporate orientation?

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Inside onCreate");

        /* Initialize sensor handles */
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope     = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMagnet        = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorCounter = 0;

        /* Initialize wearable connection */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "Connected to GoogleApi");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "Connection to GoogleApi suspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(TAG, "Connection to GoogleApi failed");
                        // TODO: Reinitialize connection
                    }
                })
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String messagePath = messageEvent.getPath();

        /* Message handling */
        if (messagePath.equals(START_PATH)) {
            Log.d(TAG, "Received message, starting watch collection");
            registerListeners();

        } else if (messagePath.equals(STOP_COLLECTION_PATH) | messagePath.equals(STOP_SERVICE_PATH)) {
            Log.d(TAG, "Received message, stopping watch collection");
            mSensorManager.unregisterListener(this);

        } else {
            Log.d(TAG, "Received message: " + messagePath);
        }
    }

    public void registerListeners() {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnet, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /******* SensorEventListener Methods ********/

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mGoogleApiClient.isConnected()) {
            Log.d(TAG, "Sensor event: " + event.sensor.getName());
            sendSensorDataToPhone(event);

        } else {
            Log.d(TAG, "GoogleApiClient not connected during SensorEvent");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // STUB
    }

    /******* Helper Methods *******/
    private void sendSensorDataToPhone(SensorEvent event) {
        /* Create map to hold sensor packet */
        if (mSensorCounter == 0) {
            sensorDataMap = PutDataMapRequest.create("/sensor");
        }

        /* Populate packet */
        String counterString = Integer.toString(mSensorCounter);
        String valKey = "values" + counterString;
        String typeKey = "sensor_type" + counterString;
        String timestampKey = "timestamp" + counterString;

        sensorDataMap.getDataMap().putFloatArray(valKey, event.values.clone());
        sensorDataMap.getDataMap().putInt(typeKey, event.sensor.getType());
        sensorDataMap.getDataMap().putLong(timestampKey, event.timestamp);

        /* If full, send packet */
        if (mSensorCounter == MAX_EVENTS_IN_PACKET) {

        }
    }
}
