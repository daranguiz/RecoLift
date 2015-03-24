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
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;


/* This class implements the googleApiClient communication layer,
 * as well as the watch sensor collection.
 */
public class WatchDataLayerListenerService extends WearableListenerService
        implements SensorEventListener {
    public WatchDataLayerListenerService() {
    }

    private final String TAG = "DataLayerListenerServ";
    private GoogleApiClient mGoogleApiClient;

    /* Sensor Global Variables */
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mMagnet;
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

        /* Initialize wearable connection */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "Connected to GoogleApi");
                        registerListeners();
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
        String messagePath = new String(messageEvent.getPath());
        Log.d(TAG, "Received message: " + messagePath);
    }

    public void registerListeners() {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnet, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /******* SensorEventListener Methods ********/

    @Override
    public void onSensorChanged(SensorEvent event) {
        // STUB
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // STUB
    }
}
