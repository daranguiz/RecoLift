package com.daranguiz.sensordatacollection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;

import java.util.List;

public class WatchDataCollection extends Activity {

    private TextView mTextView;
    private GoogleApiClient mGoogleApiClient;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private boolean mResolvingError = false;

    /* just sensor things */
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mMagnet;
    private Sensor mRotVector;
    private Sensor mOrientation;
    private Sensor mGravity;
    private Sensor mLinearAcceleration;

    float[] lastAccelerometerData = {0f, 0f, 0f};
    float[] lastGyroscopeData = {0f, 0f, 0f};
    float[] lastMagnetData = {0f, 0f, 0f};
    float[] lastRotVectorData = {0f, 0f, 0f, 0f, 0f};
    float[] lastOrientationData = {0f, 0f, 0f};
    float[] lastGravityData = {0f, 0f, 0f};
    float[] lastLinearAccelerationData = {0f, 0f, 0f};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_data_collection);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mTextView.setText("Listener service started");
            }
        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        // list iterator to see what is available
        for (Sensor curSensor : deviceSensors) {
//            Log.d("SensorType", curSensor.getStringType());
        }

        Log.d("DEBUG", "Tryna start service");
        Intent sensorServiceIntent = new Intent(this, SensorBackgroundService.class);
        startService(sensorServiceIntent);
    }

    @Override
    protected void onResume()
    {
        Log.d("DEBUG", "RESUMED!");
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        Log.d("DEBUG", "PAUSED!");
        super.onPause();
    }


    @Override
    protected void onStop() {
//        mGoogleApiClient.disconnect();
        Log.d("DEBUG", "onStop called");
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("DEBUG", "onStart called");
        //new Thread(new GetContent()).start();
    }


    private void showErrorDialog(int errorCode) {
        mTextView.setText("\n    Error" + Integer.toString(errorCode));
    }
}
