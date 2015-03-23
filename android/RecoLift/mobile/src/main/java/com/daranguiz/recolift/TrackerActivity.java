package com.daranguiz.recolift;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;


public class TrackerActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private static String TAG = "TrackerActivity";
    public static String START_PATH = "config/start";
    public static String STOP_PATH = "config/stop";
    Intent phoneListenerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        /* Layout init */
        // TODO: Layout Init

        /* Start service to initialize GoogleAPI connections */
        phoneListenerService = new Intent(getApplicationContext(), DataLayerListenerService.class);
        startService(phoneListenerService);

        /* Init GoogleAPI for Activity<->Service communication */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        /* Begin GoogleAPI connection */
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
            Log.d(TAG, "Connecting to GoogleApiClient on TrackerActivity");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tracker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Destroying TrackerActivity");

        // NOTE: This may not actually work, may need to send message
        // and allow it to stop on its own
//        stopService(phoneListenerService);

        // Send STOP command to service
        // NOTE: Do I actually want to do this? This should be a button.
        // what if it stops whenever the app loses focus? (note it doesn't)
        new SendMessageActivityToService(STOP_PATH, "Stop").start();

        super.onDestroy();
    }

    /********** GoogleApiClient.ConnectionCallbacks **********/
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "GoogleApiClient connected successfully");
        new SendMessageActivityToService(START_PATH, "Start").start();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "GoogleApiClient connection suspended");
    }

    /********* GoogleApiClient.OnConnectionFailedListener *********/
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // TODO: attempt reconnect
        Log.d(TAG, "GoogleApiClient failed to connect");
    }

    /********* Helper functions *********/


    // http://stackoverflow.com/questions/26479193/sending-data-from-an-activity-to-wearablelistenerservice
    class SendMessageActivityToService extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendMessageActivityToService(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetLocalNodeResult nodes = Wearable.NodeApi.getLocalNode(mGoogleApiClient).await(); // block
            Node node = nodes.getNode();
            Log.d(TAG, "Activity node is: " + node.getId() + " - " + node.getDisplayName());
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message.getBytes()).await();
            if (result.getStatus().isSuccess()) {
                Log.d(TAG, "Activity message: {" + message + "} sent to: " + node.getDisplayName());
            } else {
                Log.e(TAG, "Failed to send Activity message");
            }
        }
    }
}
