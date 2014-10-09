package com.daranguiz.initialdatacollection;

import android.app.Activity;
import android.content.Context;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class WatchDataCollection extends Activity implements SensorEventListener {

    private TextView mTextView;

    /* just sensor things */
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mMagnet;
    private Sensor mLight;
    private Sensor mRotVector;
    private Sensor mOrientation;
    private Sensor mGravity;
    private Sensor mLinearAcceleration;

    String filename = "sensor_data.csv";
    FileWriter writer;

    float[] lastAccelerometerData = {0f, 0f, 0f};
    float[] lastGyroscopeData = {0f, 0f, 0f};
    float[] lastMagnetData = {0f, 0f, 0f};
    float lastLightData = 0f;
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
            }
        });

        mTextView = new TextView(this);
        mTextView.setTextSize(40);

        // creates the file
        try
        {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            if (!path.exists()) {
                path.mkdir();
            }
//            File file = new File("/sdcard/DCIM/test_csv.txt");
            File file = new File(path, "test_csv.txt");
            Log.d("Path", file.getAbsolutePath());
            file.createNewFile();
            writer = new FileWriter(file);
            mTextView.setText("New CSV file creation succeeded, check /storage/emulated/0/DCIM/test_csv.txt after data collection.");
        }
        catch (Exception e)
        {
            mTextView.setText("CSV file creation failed, perhaps device is still mounted as mass storage?");
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        // list iterator to see what is available
        for (Sensor curSensor : deviceSensors) {
            Log.d("SensorType", curSensor.getStringType());
        }

        // Set the text view as the activity layout
        setContentView(mTextView);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMagnet = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mRotVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mMagnet, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mRotVector, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mLinearAcceleration, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        /* Columns:
         * Timestamp (in milliseconds)
         * Accel {0, 1, 2}
         * Gyro {0, 1, 2}
         * Magnet {0, 1, 2}
         * Light {0, 1, 2}
         * RotVector {0, 1, 2, 3, 4}
         * Orientation {0, 1, 2}
         * Gravity {0, 1, 2}
         * LinearAcceleration {0, 1, 2}
         */

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, lastAccelerometerData, 0, event.values.length);
                break;
            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(event.values, 0, lastGyroscopeData, 0, event.values.length);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, lastMagnetData, 0, event.values.length);
                break;
            case Sensor.TYPE_LIGHT:
                lastLightData = event.values[0];
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                System.arraycopy(event.values, 0, lastRotVectorData, 0, event.values.length);
                break;
            case Sensor.TYPE_ORIENTATION:
                System.arraycopy(event.values, 0, lastOrientationData, 0, event.values.length);
                break;
            case Sensor.TYPE_GRAVITY:
                System.arraycopy(event.values, 0, lastGravityData, 0, event.values.length);
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                System.arraycopy(event.values, 0, lastLinearAccelerationData, 0, event.values.length);
                break;
            default:
                break;
        }

        String newEntry = writeNewCSVEntryFromSensors();

        try {
            writer.write(newEntry);
            writer.flush();
        }
        catch (Exception e) {
            //
        }
    }

    private String writeNewCSVEntryFromSensors() {
        String newEntry = String.valueOf(System.currentTimeMillis()) + ", ";

        for (float sensorVal : lastAccelerometerData) {
            newEntry += Float.toString(sensorVal) + ", ";
        }
        for (float sensorVal : lastGyroscopeData) {
            newEntry += Float.toString(sensorVal) + ", ";
        }
        for (float sensorVal : lastMagnetData) {
            newEntry += Float.toString(sensorVal) + ", ";
        }
        newEntry += Float.toString(lastLightData) + ", ";
        for (float sensorVal : lastRotVectorData) {
            newEntry += Float.toString(sensorVal) + ", ";
        }
        for (float sensorVal : lastOrientationData) {
            newEntry += Float.toString(sensorVal) + ", ";
        }
        for (float sensorVal : lastGravityData) {
            newEntry += Float.toString(sensorVal) + ", ";
        }
        for (float sensorVal : lastLinearAccelerationData) {
            newEntry += Float.toString(sensorVal) + ", ";
        }

        newEntry += "\r\n";

        return newEntry;
    }
}
