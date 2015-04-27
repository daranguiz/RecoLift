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
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;


/* This class implements the googleApiClient communication layer,
 * as well as the watch sensor collection.
 */
public class WatchDataLayerListenerService extends WearableListenerService
        implements SensorEventListener {
    public WatchDataLayerListenerService() {
    }

    /* Constants */
    private static final String TAG = "DataLayerListenerServ";
    public static String START_PATH = "config/start";
    public static String STOP_SERVICE_PATH = "config/stop_service";
    public static String STOP_COLLECTION_PATH = "config/stop_collection";
    public static final int MAX_EVENTS_IN_PACKET = 20;

    /* Sensor types, match the enum in /mobile/src/datatypes/SensorType */
    private static final int SENSOR_ACCEL = 1;
    private static final int SENSOR_GYRO = 3;

    /* Communication */
    private GoogleApiClient mGoogleApiClient;
    private String lastMessage;

    /* Sensor Global Variables */
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
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
        lastMessage = "";
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        lastMessage = messageEvent.getPath();

        /* Message handling */
        if (lastMessage.equals(START_PATH)) {
            Log.d(TAG, "Received message, starting watch collection");
            registerListeners();
            mSensorCounter = 0;

        } else if (lastMessage.equals(STOP_COLLECTION_PATH) | lastMessage.equals(STOP_SERVICE_PATH)) {
            Log.d(TAG, "Received message, stopping watch collection");
            mSensorManager.unregisterListener(this);

        } else {
            Log.d(TAG, "Received message: " + lastMessage);
        }
    }

    public void registerListeners() {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
//        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
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

        /* Get sensor type to send to phone */
        int sensorType = -1;
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                sensorType = SENSOR_ACCEL;
                break;
            case Sensor.TYPE_GYROSCOPE:
                sensorType = SENSOR_GYRO;
                break;
            default:
                sensorType = -1;
        }

        if (sensorType == -1) {
            Log.d(TAG, "Non-gyro/accelerometer event, not sending");
            return;
        }

        sensorDataMap.getDataMap().putFloatArray(valKey, event.values.clone());
        sensorDataMap.getDataMap().putInt(typeKey, sensorType);
        sensorDataMap.getDataMap().putLong(timestampKey, event.timestamp);
        mSensorCounter++;

        /* If full, send packet */
        if (mSensorCounter >= MAX_EVENTS_IN_PACKET) {
            Log.d(TAG, "Sending sensor data packet");
            PutDataRequest request = sensorDataMap.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                    .putDataItem(mGoogleApiClient, request);
            mSensorCounter = 0;
        }
    }

    /* Quick notes before I leave:
       Use ZOH for resampling. On phone side, keep track of the last received sensor value from
       a given sensor. Every 100ms (10Hz, something), set the value equal to the last received value.
       This may take some doing, as sensor values are received in a bundle. Perhaps ZOH-resample
       within bundle, then keep track of last value received in the bundle to resample subsequent
       values? But then at that point, why not just linearly interpolate. meh. thoughts.

       TODO: ZOH first, linear after ZOH finished
     */
}
