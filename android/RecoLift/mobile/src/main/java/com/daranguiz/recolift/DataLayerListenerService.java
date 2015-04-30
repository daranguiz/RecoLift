package com.daranguiz.recolift;

import android.hardware.Sensor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.daranguiz.recolift.datatype.SensorType;
import com.daranguiz.recolift.utils.ButterworthLowPassFilter;
import com.daranguiz.recolift.datatype.SensorValue;
import com.daranguiz.recolift.utils.CountingPhase;
import com.daranguiz.recolift.utils.RecognitionPhase;
import com.daranguiz.recolift.utils.SegmentationPhase;
import com.daranguiz.recolift.utils.ZohResample;
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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

public class DataLayerListenerService extends WearableListenerService {
    public DataLayerListenerService() {
    }

    /* Constants */
    private static final String TAG = "RecoDataLayerListenerServ";
    private static final int MAX_EVENTS_IN_PACKET = 20;
    private static final int SEC_TO_NS = 1000000000;
    private static final int MS_TO_NS = 1000000;
    private static final int F_S = 25;
    private static final int SAMPLING_DELTA_NS = Math.round(SEC_TO_NS * 0.04f);
    private static final int NUM_DOFS = 3;

    private GoogleApiClient mGoogleApiClient;
    private String lastMessageSent;

    /* Sensor values */
    private SensorValue lastSensorValue;
    public Map<SensorType, List<SensorValue>> mSensorData;
    private static final SensorType[] sensorTypeCache = SensorType.values();

    /* Processing */
    private Map<SensorType, ButterworthLowPassFilter[]> mLowPassFilters;
    private Map<SensorType, ZohResample> mZohResamplers;
    private SegmentationPhase mSegmentationPhase;
    private RecognitionPhase mRecognitionPhase;
    private CountingPhase mCountingPhase;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service started");

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

        /* Sensor data init */
        mSensorData = new TreeMap<>();
        for (SensorType sensor : SensorType.values()) {
            mSensorData.put(sensor, new Vector<SensorValue>());
        }

        /* Phase init */
        mSegmentationPhase = new SegmentationPhase(this, mSensorData);
        mRecognitionPhase = new RecognitionPhase(this, mSensorData);
        mCountingPhase = new CountingPhase(mSensorData);

        /* Filter init */
        mLowPassFilters = new TreeMap<>();
        for (SensorType sensor : sensorTypeCache) {
            mLowPassFilters.put(sensor, new ButterworthLowPassFilter[NUM_DOFS]);
            for (int i = 0; i < NUM_DOFS; i++) {
                mLowPassFilters.get(sensor)[i] = new ButterworthLowPassFilter();
            }
        }

        /* Resampler init */
        float[] emptyValues = {-1, -1, -1};
        lastSensorValue = new SensorValue(-1, emptyValues);
        mZohResamplers = new TreeMap<>();
        for (SensorType sensor : sensorTypeCache) {
            mZohResamplers.put(sensor, new ZohResample(lastSensorValue, SAMPLING_DELTA_NS));
        }

        /* Other init */
        lastMessageSent = "";
    }

    @Override
    // TODO: Keep the watch from sleeping somehow, it stops transmitting data after some period of inactivity
    // ^ maybe that doesn't matter if you're in the gym, will be moving and not inactive ever?
    public void onDataChanged(DataEventBuffer dataEvents) {
        // TODO: Write to CSV, talk to server?
//        Log.d(TAG, "Received sensor data");

        /* If new sensor data has been received */
        if (lastMessageSent.equals(TrackerActivity.START_PATH)) {
            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_DELETED) {
                    Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());

                } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataMap receivedDataMap = DataMap.fromByteArray(event.getDataItem().getData());

                    for (int i = 0; i < MAX_EVENTS_IN_PACKET; i++) {
                        String counterString = Integer.toString(i);
                        String valKey = "values" + counterString;
                        String typeKey = "sensor_type" + Integer.toString(i);
                        String timestampKey = "timestamp" + Integer.toString(i);

                        /* Parse old data */
                        float[] dataArray = receivedDataMap.getFloatArray(valKey);
                        SensorType dataSensorType = sensorTypeCache[receivedDataMap.getInt(typeKey)];
                        long dataTimestamp = receivedDataMap.getLong(timestampKey);
                        SensorValue curSensorValue = new SensorValue(dataTimestamp, dataArray);

                        // TODO: HANDLE PHONE SENSORS AS WELL AS WATCH

                        /* Resample to 25Hz with ZOH */
                        mSensorData.get(dataSensorType).add(mZohResamplers.get(dataSensorType).resample(curSensorValue));

                        /* Filter just-added sample */
                        int curIdx = mSensorData.get(dataSensorType).size() - 1;
                        SensorValue curSensorVal = mSensorData.get(dataSensorType).get(curIdx);
                        ButterworthLowPassFilter[] curFilts = mLowPassFilters.get(dataSensorType);
                        for (int j = 0; j < NUM_DOFS; j++) {
                            float outputVal = curFilts[j].singleFilt(curSensorVal.values[j]);
                            curSensorVal.values[j] = outputVal;

                            /* Logging! Quick test */
                            if (j == 0 & false) {
                                Log.d(TAG, "XVal: " + outputVal);
                            }
                        }
                        mSensorData.get(dataSensorType).set(curIdx, curSensorVal);
                    }
                }
            }

            /* Begin segmentation */
            if (mSegmentationPhase.performBatchSegmentation()) {
                /* If we've seen a full exercise window, let recognition know */
//                Log.d(TAG, "Full exercise seen!");
//                Toast.makeText(getApplicationContext(), "Full exercise seen!", Toast.LENGTH_SHORT).show();

                int startIdx = mSegmentationPhase.fullStartLiftIdx;
                int stopIdx = mSegmentationPhase.fullStopLiftIdx;

                /* Run recognition over window */
                String curLift = mRecognitionPhase.performBatchRecognition(startIdx, stopIdx);
                Log.d(TAG, "Lift classified: " + curLift);
//                Toast.makeText(getApplicationContext(), "Completed lift: " + curLift, Toast.LENGTH_SHORT).show();

                /* Run counting! */
                int numReps = mCountingPhase.performBatchCounting(startIdx, stopIdx, curLift);

                Log.d(TAG, "Num reps: " + numReps);
                Toast.makeText(getApplicationContext(), curLift + ": " + numReps + " reps", Toast.LENGTH_LONG).show();
            }

            /* Run recognition anyway to get ground truth data */
//            mRecognitionPhase.performBatchRecognition(0, 0);
//
//            /* Run counting over the last 5 seconds anyway */
//            if (mSensorData.get(SensorType.ACCEL_WATCH).size() > F_S * 5) {
//                mCountingPhase.performBatchCounting(mSensorData.get(SensorType.ACCEL_WATCH).size() - F_S * 5,
//                        mSensorData.get(SensorType.ACCEL_WATCH).size() - 1, "placeholderLiftType");
//            }
        }

    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();

        // This takes maybe 5 seconds or so
        Log.d(TAG, "Service destroyed");
        super.onDestroy();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
//        Log.d(TAG, "Received message");

        if (messageEvent.getPath().equals(TrackerActivity.START_PATH)) {
            // TODO: Begin sensor tracking and computation
            Log.d(TAG, "Received START message");
            sendWatchMessage(TrackerActivity.START_PATH);

            // TODO: Register sensors on phone locally
            // TODO: Open CSV? Communicate with DigitalOcean?

        } else if (messageEvent.getPath().equals(TrackerActivity.STOP_COLLECTION_PATH)) {
            // TODO: Deregister sensors and stop collection
            Log.d(TAG, "Received STOP_COLLECTION message");
            sendWatchMessage(TrackerActivity.STOP_COLLECTION_PATH);

            // TODO: Deregister sensors on phone locally

        } else if (messageEvent.getPath().equals(TrackerActivity.STOP_SERVICE_PATH)) {
            // TODO: Stop service
            Log.d(TAG, "Received STOP message");
            sendWatchMessage(TrackerActivity.STOP_SERVICE_PATH);

            // Kill mGoogleApiClient and stop service
            mGoogleApiClient.disconnect();
            stopSelf();
        }
    }

    private Collection<String> sendWatchMessage(String message) {
        Log.d(TAG, "Inside sendWatchMessage");
        lastMessageSent = message;

        // Create container for connected nodes
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        // Get handle to local node, ie phone
        NodeApi.GetLocalNodeResult myNode =
                Wearable.NodeApi.getLocalNode(mGoogleApiClient).await();
        Log.d(TAG, "myNodeID = " + myNode.getNode().getId());

        // Get connected nodes' ID strings
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        // Send message to all connected nodes
        for (String id : results) {
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, id, message, null).await();

            // Display result of message send
            if (!result.getStatus().isSuccess()) {
                Log.e(TAG, "ERROR: Failed to send message: " + result.getStatus());
            } else {
                Log.d(TAG, "Message: {" + message + "} sent to: " + id);
            }
        }

        // Return list of connected nodes' IDs
        return results;
    }

}
