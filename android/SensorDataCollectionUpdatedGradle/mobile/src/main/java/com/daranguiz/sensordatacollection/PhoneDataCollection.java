package com.daranguiz.sensordatacollection;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.daranguiz.sensordatacollection.PhoneDataLayerListenerService.PhoneDataLayerListenerBinder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedWriter;
import java.io.File;


public class PhoneDataCollection extends Activity implements
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError;
    private static final String START_PATH = "config/start";
    private static final String STOP_PATH = "config/stop";
    private TextView[] mTextView = new TextView[5];
    private EditText mEditText;
    private boolean mSensorCSVFileOpen;
    private File mSensorCSVFile;
    private BufferedWriter mSensorCSVFileWriter;

    // Service handler
    PhoneDataLayerListenerBinder mBinder;
    PhoneDataLayerListenerService mService;
    boolean mBound;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    String TAG = "DEBUG";
    Intent phoneListenerServiceIntent;
    private boolean serviceStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBound = false;
        serviceStarted = false;
        mSensorCSVFileOpen = false;
        setContentView(R.layout.activity_phone_data_collection);

        String tempCSVName = "sensor_csv_" + Long.toString(System.currentTimeMillis());
        mEditText = (EditText) findViewById(R.id.text_box_editable_csv_name);
        mEditText.setText(tempCSVName);

        Button button_start_sensor = (Button) findViewById(R.id.button_start_sensor);
        button_start_sensor.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Start data collection
                new SendMessageActivityToService(START_PATH, mEditText.getText().toString().trim()).start();
            }
        });

        Button button_stop_sensor = (Button) findViewById(R.id.button_stop_sensor);
        button_stop_sensor.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Stop data collection
                new SendMessageActivityToService(STOP_PATH, "test").start();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
            Log.v(TAG, "Connecting to GoogleApiClient on Activity");
        }

        Log.d(TAG, "Tryna start service");
        phoneListenerServiceIntent = new Intent(getApplicationContext(), PhoneDataLayerListenerService.class);
//        phoneListenerServiceIntent.putExtra("textBoxInput", mEditText.getText().toString().trim());
        startService(phoneListenerServiceIntent);
        bindService(phoneListenerServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Tryna stop service");
        stopService(phoneListenerServiceIntent);
        unbindService(mConnection);
    }

    /* Defines callbacks for service binding, passed to bindService(). Never called for some reason? */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Log.d(TAG, "Service connected");
            mBinder = (PhoneDataLayerListenerBinder) service;
            mService = mBinder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    @Override
    public void onConnectionSuspended(int cause) {
        Log.v(TAG, "onConnectionSuspended called in Activity");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(TAG, "onConnectionFailed called in Activity");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.v(TAG, "onConnected called in Activity");
    }

    /* http://stackoverflow.com/questions/26479193/sending-data-from-an-activity-to-wearablelistenerservice */
    class SendMessageActivityToService extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendMessageActivityToService(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetLocalNodeResult nodes = Wearable.NodeApi.getLocalNode(mGoogleApiClient).await();
            Node node = nodes.getNode();
            Log.d(TAG, "Activity Node is: " + node.getId() + " - " + node.getDisplayName());
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message.getBytes()).await();
            if (result.getStatus().isSuccess()) {
                Log.d(TAG, "Activity Message: {" + message + "} sent to: " + node.getDisplayName());
            } else {
                Log.d(TAG, "ERROR: Failed to send Activity Message");
            }
        }
    }
}
