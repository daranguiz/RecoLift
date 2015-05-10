package com.daranguiz.recolift;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class TrackerActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /* Constants */
    private static String TAG = "TrackerActivity";
    public static String START_PATH = "config/start";
    public static String STOP_SERVICE_PATH = "config/stop_service";
    public static String STOP_COLLECTION_PATH = "config/stop_collection";

    /* UI */
    @InjectView(R.id.textviewRecordedLifts) TextView recordedLiftsText;
    @InjectView(R.id.textviewConnectivityStatus) TextView connectivityStatusText;
    private BroadcastReceiver broadcastReceiver;
    private static String totalLiftString;

    private Intent phoneListenerService;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        /* Layout init */
        ButterKnife.inject(this);
        totalLiftString = "";

        /* Start service to initialize GoogleAPI connections */
        // TODO: This is very fragile. Hitting back button then trying to restart service kills the service permanently.
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

        /* Start BroadcastReceiver to receive UI updates */
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String liftResult = intent.getStringExtra(DataLayerListenerService.RECO_LIFT_MESSAGE);
                String sensorStatus = intent.getStringExtra(DataLayerListenerService.RECO_SENSOR_STATUS_MESSAGE);

                /* Update UI */
                updateUiFromService(liftResult, sensorStatus);
                Log.d(TAG, "Received data from Broadcast");
            }
        };
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

        // Send STOP command to service
        // NOTE: Do I actually want to do this onDestroy()? This should be a button.
        // what if it stops whenever the app loses focus? (note it doesn't)
        new SendMessageActivityToService(STOP_SERVICE_PATH, "StopService").start();

        // Important note to self:
        // Cannot stop service until googleApiClient disconnected

        super.onDestroy();
    }

    /* Needed for broadcast receiver */
    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(DataLayerListenerService.RECO_RESULT)
        );
    }

    /* Needed for broadcast receiver */
    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onStop();
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

    private void updateUiFromService(String liftResult, String sensorStatus) {
        if (liftResult != null) {
            recordedLiftsText.setText(liftResult);
        }

        if (sensorStatus != null) {
            connectivityStatusText.setText(sensorStatus);
        }
    }

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
