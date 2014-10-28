package com.daranguiz.sensordatacollection;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;


public class PhoneDataCollection extends Activity implements
        DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorCSVFileOpen = false;
        setContentView(R.layout.activity_phone_data_collection);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

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

        if (!mResolvingError) {
            mGoogleApiClient.connect();
            Log.d("DEBUG", "Attempting to connect");
        }

        Button button_start_sensor = (Button) findViewById(R.id.button_start_sensor);
        button_start_sensor.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        sendWatchRPC("StartCollection");
                        if (!mSensorCSVFileOpen) {
                            try {
                                mSensorCSVFileOpen = true;
                                mSensorCSVFile = getDCIMStorageDir(mEditText.getText() + ".csv");

                                mSensorCSVFileWriter = new BufferedWriter(new FileWriter(mSensorCSVFile));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mTextView[0].setText("CSV opened!");
                                    }
                                });
                            } catch (IOException e) {
                                Log.e(TAG, "File could not be opened");
                                Log.e(TAG, "Error message: " + e.getMessage());
                                mSensorCSVFileOpen = false;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mTextView[0].setText("CSV could not be opened");
                                    }
                                });
                            }
                        }
                    }
                }).start();
            }
        });

        Button button_stop_sensor = (Button) findViewById(R.id.button_stop_sensor);
        button_stop_sensor.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        sendWatchRPC("StopCollection");
                        if (mSensorCSVFileOpen) {
                            mSensorCSVFileOpen = false;
                            try {
                                mSensorCSVFileWriter.close();
                            } catch (IOException e) {
                                Log.e(TAG, "File could not be closed");
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTextView[0].setText("CSV closed");
                                }
                            });
                        }
                    }
                }).start();
            }
        });
    }

    private Collection<String> sendWatchRPC(String message) {
        Log.d("DEBUG", "Inside sendWatchRPC");
        HashSet<String> results= new HashSet<String>();
        NodeApi.GetLocalNodeResult myNode =
                Wearable.NodeApi.getLocalNode(mGoogleApiClient).await();
        Log.d(TAG, "myNodeID = " + myNode.getNode().getId());
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
            Log.d("DEBUG", "nodeID = " + node.getId());
        }
        for (String id : results) {
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, id, message, null).await();
            if (!result.getStatus().isSuccess()) {
                Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
            } else {
                Log.d(TAG, "Message: {" + message + "} sent to: " + id);
            }
        }
        return results;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("DEBUG", "onStart");
        if (!mResolvingError) {
            mGoogleApiClient.connect();
            Log.d("DEBUG", "Attempting to connect");
        }
    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
                DataMap dataFromWatch = DataMap.fromByteArray(event.getDataItem().getData());
                final float[] dataArray = dataFromWatch.getFloatArray("values");
                final int dataType = dataFromWatch.getInt("sensor_type");
                final long dataTimestamp = dataFromWatch.getLong("timestamp");
                if (mSensorCSVFileOpen) {
                    String newLine = "";
                    newLine += Integer.toString(dataType) + ", ";
                    newLine += Long.toString(dataTimestamp) + ", ";
                    for (float sensorData : dataArray) {
                        newLine += Float.toString(sensorData) + ", ";
                    }
                    // Remove the last extra comma
                    newLine = newLine.substring(0, newLine.length() - 2) + "\r\n";
                    try {
                        mSensorCSVFileWriter.write(newLine);
                    } catch (IOException e) {
                        Log.e(TAG, "File could not be written");
                    }
                }
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        for (int i = 0; i < dataArray.length && i < 5; i++) {
//                            mTextView[i].setText(Float.toString(dataArray[i]));
//                        }
//                    }
//                });
            }
//            mTextView.setText(event.getDataItem().getUri());
        }


    }

    /* GoogleApiClient Callbacks */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("DEBUG", "Connected to Google Api Service");

        // Maybe this should be an AsyncTask? not too sure of the difference
//        new Thread(new Runnable() {
//            public void run() {
//                getNodes();
//            }
//        }).start();

        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.d("DEBUG", "Inside onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        Log.d("DEBUG", "Connection failed");
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    private void showErrorDialog(int errorCode) {
        mTextView[0].setText("\n    Error" + Integer.toString(errorCode));
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public File getDCIMStorageDir(String newFileName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), newFileName);
        return file;
    }
}
