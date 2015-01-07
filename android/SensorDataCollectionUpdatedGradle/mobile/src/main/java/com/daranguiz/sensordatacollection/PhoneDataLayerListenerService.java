package com.daranguiz.sensordatacollection;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

public class PhoneDataLayerListenerService extends WearableListenerService {
    public PhoneDataLayerListenerService() {
    }

    private final IBinder mBinder = new PhoneDataLayerListenerBinder();
    private static final String TAG = "sDEBUG";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";
    private static final String START_PATH = "config/start";
    private static final String STOP_PATH = "config/stop";
    private static final int NUM_VALUES_IN_PACKET = 20;

    private GoogleApiClient mGoogleApiClient;
    private boolean serviceStatus;
    private boolean channelShutdown;
    private boolean mResolvingError;
    private TextView[] mTextView = new TextView[5];
    private EditText mEditText;
    private boolean mSensorCSVFileOpen;
    private File mSensorCSVFile;
    private BufferedWriter mSensorCSVFileWriter;

    PowerManager mgr;
    PowerManager.WakeLock wakeLock;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    public class PhoneDataLayerListenerBinder extends Binder {
        PhoneDataLayerListenerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PhoneDataLayerListenerService.this;
        }
        void setChannelShutdownState(boolean val) {
           channelShutdown = val;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mSensorCSVFileOpen = false;
        serviceStatus = true;
        Log.d(TAG, "onCreate");

        // http://stackoverflow.com/questions/6091270/how-can-i-keep-my-android-service-running-when-the-screen-is-turned-off
        PowerManager mgr = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
//        wakeLock.acquire();

        Log.d(TAG, "onStartCommand");



        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() { // Do I even need these?
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "Connected to play services!");
                        new Thread(new Runnable() {
                            public void run() {
//                                sendWatchRPC("StartCollection");
                            }
                        }).start();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d("sDEBUG", "Disconnected from play services");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d("sDEBUG", "Connection failed");
                    }
                })
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        if (messageEvent.getPath().equals(START_PATH)) {
            Log.d(TAG, "Start message received!");
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire();
            }
            sendWatchRPC("StartCollection");
            try {
                if (!mSensorCSVFileOpen) {
                    mSensorCSVFileOpen = true;
                    String fromTextBox = new String(messageEvent.getData());
                    Log.d(TAG, "From text box: " + fromTextBox);
                    mSensorCSVFile = getDCIMStorageDir(fromTextBox + ".csv");
                    mSensorCSVFileWriter = new BufferedWriter(new FileWriter(mSensorCSVFile));
                }
            } catch (Exception e) {
                Log.e(TAG, "File could not be opened");
                Log.e(TAG, "Error message: " + e.getMessage());
                mSensorCSVFileOpen = false;
            }
        } else if (messageEvent.getPath().equals(STOP_PATH)) {
            Log.d(TAG, "Stop message received!");
            sendWatchRPC("StopCollection");
            try {
                mSensorCSVFileWriter.close();
                mSensorCSVFileOpen = false;
            } catch (Exception e) {
                Log.e(TAG, "File could not be closed");
            }
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "Inside onDataChanged ");
//        stopSelf();

        if (serviceStatus) {
            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_DELETED) {
                    Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
                } else if (event.getType() == DataEvent.TYPE_CHANGED) {
//                                    Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
                    DataMap dataMap = DataMap.fromByteArray(event.getDataItem().getData());
                    for (int i = 0; i < NUM_VALUES_IN_PACKET; i++) {
                        String valKey = "values" + Integer.toString(i);
                        String typeKey = "sensor_type" + Integer.toString(i);
                        String timestampKey = "timestamp" + Integer.toString(i);

                        float[] dataArray = dataMap.getFloatArray(valKey);
                        int dataType = dataMap.getInt(typeKey);
                        long dataTimestamp = dataMap.getLong(timestampKey);

                        //                    Log.d(TAG, Float.toString(dataArray[0]));
                        //                    Log.d(TAG, Integer.toString(dataType));
                        //                    Log.d(TAG, Long.toString(dataTimestamp));
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
                    }
                }
            }
        }
//        Log.d(TAG, "Done with onDataChanged");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying listener service");

        // Stop processing data
        serviceStatus = false;

        // Close CSV
        mSensorCSVFileOpen = false;
        try {
            mSensorCSVFileWriter.close();
        } catch (Exception e) {
            Log.e(TAG, "File could not be closed");
        }

        new Thread(new Runnable() {
            public void run() {
                mGoogleApiClient.disconnect();
                if (wakeLock != null && wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }
        }).start();

        super.onDestroy();
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

    public File getDCIMStorageDir(String newFileName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), newFileName);
        return file;
    }
}
