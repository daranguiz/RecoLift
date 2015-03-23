package com.daranguiz.recolift;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Collection;
import java.util.HashSet;

public class DataLayerListenerService extends WearableListenerService {
    public DataLayerListenerService() {
    }

    private String TAG = "DataLayerListenerService";
    private GoogleApiClient mGoogleApiClient;

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
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        // TODO: Write to CSV, talk to server?
        Log.d(TAG, "Received sensor data");

        super.onDataChanged(dataEvents);
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
