package com.daranguiz.sensordatacollection;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedWriter;
import java.io.File;


public class PhoneDataCollection extends Activity {

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError;
    private TextView[] mTextView = new TextView[5];
    private EditText mEditText;
    private boolean mSensorCSVFileOpen;
    private File mSensorCSVFile;
    private BufferedWriter mSensorCSVFileWriter;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    String TAG = "DEBUG";
    Intent phoneListenerServiceIntent;
    private boolean serviceStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        serviceStarted = false;
        mSensorCSVFileOpen = false;
        setContentView(R.layout.activity_phone_data_collection);

        mTextView[0] = (TextView) findViewById(R.id.text_box_1);
        mTextView[0].setText("TEST");
        mTextView[1] = (TextView) findViewById(R.id.text_box_2);
        mTextView[1].setText("TEST");
        mTextView[2] = (TextView) findViewById(R.id.text_box_3);
        mTextView[2].setText("TEST");
        mTextView[3] = (TextView) findViewById(R.id.text_box_4);
        mTextView[3].setText("TEST");
        mTextView[4] = (TextView) findViewById(R.id.text_box_5);
        mTextView[4].setText("TEST");

        String tempCSVName = "sensor_csv_" + Long.toString(System.currentTimeMillis());
        mEditText = (EditText) findViewById(R.id.text_box_editable_csv_name);
        mEditText.setText(tempCSVName);

        Button button_start_sensor = (Button) findViewById(R.id.button_start_sensor);
        button_start_sensor.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Start intent
//                if (!serviceStarted) {
                    Log.d(TAG, "Tryna start service");
                    phoneListenerServiceIntent = new Intent(v.getContext(), PhoneDataLayerListenerService.class);
                    phoneListenerServiceIntent.putExtra("textBoxInput", mEditText.getText().toString().trim());
                    startService(phoneListenerServiceIntent);
                    serviceStarted = true;
                }
//            }
        });

        Button button_stop_sensor = (Button) findViewById(R.id.button_stop_sensor);
        button_stop_sensor.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Stop intent
//                if (serviceStarted) {
                    Log.d(TAG, "Tryna stop service");
                    boolean serviceStopped = stopService(new Intent(v.getContext(), PhoneDataLayerListenerService.class));
//                    boolean serviceStopped = stopService(phoneListenerServiceIntent);
                    Log.d(TAG, "Service stopped = " + Boolean.toString(serviceStopped));
                    serviceStarted = false;
//                }
            }
        });


    }



}
