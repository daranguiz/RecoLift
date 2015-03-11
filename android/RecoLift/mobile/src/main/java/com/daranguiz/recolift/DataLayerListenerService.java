package com.daranguiz.recolift;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

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
        Log.d(TAG, "Received message");

        if (messageEvent.getPath().equals(TrackerActivity.START_PATH)) {
            // TODO: Begin sensor tracking and computation
            Log.d(TAG, "Received START message");
        } else if (messageEvent.getPath().equals(TrackerActivity.STOP_PATH)) {
            // TODO: Deregister sensors and stop collection
            Log.d(TAG, "Received STOP message");
            stopSelf();
        }
    }

}
