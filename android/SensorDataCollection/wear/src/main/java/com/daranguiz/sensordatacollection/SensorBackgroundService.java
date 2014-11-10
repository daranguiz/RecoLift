package com.daranguiz.sensordatacollection;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;

public class SensorBackgroundService extends WearableListenerService
        implements SensorEventListener {

    // #define's
    private static final boolean SENSOR_ALWAYS_ON = false;

    private static final String TAG = "sDEBUG";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

    private GoogleApiClient mGoogleApiClient;
    SensorBackgroundService curService = this;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private boolean mResolvingError = false;
    private int mSensorCounter = 0;
    private PutDataMapRequest dataMap;
    private Lock mLock;

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
    public void onCreate() {
        super.onCreate();

        Log.d("sDEBUG", "SensorService started!");

        mSensorCounter = 0;

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMagnet = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mRotVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        /* Initialize datastream connection from watch to phone */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() { // Do I even need these?
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d("sDEBUG", "Connected to play services!");
                        if (SENSOR_ALWAYS_ON) {
                            mSensorManager.registerListener(curService, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
//                            mSensorManager.registerListener(curService, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
//                            mSensorManager.registerListener(curService, mMagnet, SensorManager.SENSOR_DELAY_NORMAL);
//                            mSensorManager.registerListener(curService, mRotVector, SensorManager.SENSOR_DELAY_NORMAL);
//                            mSensorManager.registerListener(curService, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
//                            mSensorManager.registerListener(curService, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
//                            mSensorManager.registerListener(curService, mLinearAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
                        }

                        new Thread(new Runnable() {
                            public void run() {
                            listConnectedNodes();
                        }
                    }).start();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d("sDEBUG", "Disconnected from play services");
                        if (SENSOR_ALWAYS_ON) {
                            mSensorManager.unregisterListener(curService);
                        }
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d("sDEBUG", "Connection failed");
                        if (SENSOR_ALWAYS_ON) {
                            mSensorManager.unregisterListener(curService);
                        }
//                        if (mResolvingError) {
//                            // Already attempting to resolve an error.
//                            return;
//                        } else if (connectionResult.hasResolution()) {
//                            try {
//                                mResolvingError = true;
//                                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
//                            } catch (IntentSender.SendIntentException e) {
//                                // There was an error with the resolution intent. Try again.
//                                mGoogleApiClient.connect();
//                            }
//                        } else {
//                            mResolvingError = true;
//                            Log.d("sDEBUG", "Attempting to reconnect");
//                        }
                    }
                })
                .build();
        mGoogleApiClient.connect();

    }

    private Collection<String> listConnectedNodes() {
        Log.d(TAG, "Inside listConnectedNodes");
        HashSet<String> results= new HashSet<String>();
        NodeApi.GetLocalNodeResult myNode =
                Wearable.NodeApi.getLocalNode(mGoogleApiClient).await();
        Log.d(TAG, "myNodeID = " + myNode.getNode().getId());
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
            Log.d(TAG, "nodeID = " + node.getId());
        }
        return results;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // stub
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//        Log.d("sDEBUG", "SensorEvent!");
        if (mGoogleApiClient.isConnected()) {
//            Log.d("sDEBUG", Integer.toString(mSensorCounter));

            /* Populate the data te be sent to the phone */
            if (mSensorCounter == 0) {
                dataMap = PutDataMapRequest.create("/sensor");
            }

            String valKey = "values" + Integer.toString(mSensorCounter);
            String typeKey = "sensor_type" + Integer.toString(mSensorCounter);
            String timestampKey = "timestamp" + Integer.toString(mSensorCounter);

            dataMap.getDataMap().putFloatArray(valKey, event.values.clone());
            dataMap.getDataMap().putInt(typeKey, event.sensor.getType());
            dataMap.getDataMap().putLong(timestampKey, event.timestamp);
            mSensorCounter++;
//
//            Log.d(TAG, valKey);
//            Log.d(TAG, Float.toString(dataMap.getDataMap().getFloatArray(valKey)[0]));

            /* Send the data to the phone */
            if (mSensorCounter == 20) {
                PutDataRequest request = dataMap.asPutDataRequest();
                PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                        .putDataItem(mGoogleApiClient, request);
                mSensorCounter = 0;
            }
//            Log.d("sDEBUG", "SensorEvent!");
        }
    }


    /* Handle RPCs from phone, ie "Start sensor listen, stop sensor listen"
     *
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        // do a check to see that you're receiving a message from the phone, ie
        // if (messageEvent.getPath().equals(PHONE_ACTIVITY_PATH)
        String messageData = new String(messageEvent.getPath());
        Log.d(TAG, "onMessageReceived: " + messageData);
        if (messageData.equals("StartCollection")) {
            if (!SENSOR_ALWAYS_ON) {
                mSensorCounter = 0;
                mSensorManager.registerListener(curService, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
                mSensorManager.registerListener(curService, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
                mSensorManager.registerListener(curService, mMagnet, SensorManager.SENSOR_DELAY_FASTEST);
                mSensorManager.registerListener(curService, mRotVector, SensorManager.SENSOR_DELAY_FASTEST);
                mSensorManager.registerListener(curService, mOrientation, SensorManager.SENSOR_DELAY_FASTEST);
                mSensorManager.registerListener(curService, mGravity, SensorManager.SENSOR_DELAY_FASTEST);
                mSensorManager.registerListener(curService, mLinearAcceleration, SensorManager.SENSOR_DELAY_FASTEST);
            }
            Log.d("sDEBUG", "Started Sensor Collection");
        } else if (messageData.equals("StopCollection")) {
            if (!SENSOR_ALWAYS_ON) {
                mSensorManager.unregisterListener(curService);
            }
            Log.d("sDEBUG", "Stopping Sensor Collection");
        }
    }

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);

        String id = peer.getId();
        String name = peer.getDisplayName();

        Log.d(TAG, "Connected peer name & ID: " + name + "|" + id);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
//        Log.d("sDEBUG", "Inside onDataChanged in SensorBackgroundService");
//        for (DataEvent event : dataEvents) {
//            if (event.getType() == DataEvent.TYPE_DELETED) {
//                Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
//            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
//                Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
//                DataMap dataMap = DataMap.fromByteArray(event.getDataItem().getData());
//                /*for (int i = 0; i < 10; i++) {
//                    String valKey = "values" + Integer.toString(i);
//                    String typeKey = "sensor_type" + Integer.toString(i);
//                    String timestampKey = "timestamp" + Integer.toString(i);
//
//                    float[] dataArray = dataMap.getFloatArray(valKey);
//                    int dataType = dataMap.getInt(typeKey);
//                    long dataTimestamp = dataMap.getLong(timestampKey);
//
//                    Log.d(TAG, Float.toString(dataArray[0]));
////                    Log.d(TAG, Integer.toString(dataType));
//                    Log.d(TAG, Long.toString(dataTimestamp));
//                }*/
//                String valKey = "values0";
//                float[] dataArray = dataMap.getFloatArray(valKey);
//                Log.d(TAG, Float.toString(dataArray[0]));
//
//                valKey = "values1";
//                dataArray = dataMap.getFloatArray(valKey);
//                Log.d(TAG, Float.toString(dataArray[0]));
//
//                valKey = "values2";
//                dataArray = dataMap.getFloatArray(valKey);
//                Log.d(TAG, Float.toString(dataArray[0]));
//
//                valKey = "values3";
//                dataArray = dataMap.getFloatArray(valKey);
//                Log.d(TAG, Float.toString(dataArray[0]));
//            }
//        }
    }
}