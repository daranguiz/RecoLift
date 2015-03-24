package com.daranguiz.recolift;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

public class WatchDataCollection extends Activity {

    private TextView mTextView;
    private final String TAG = "WatchDataCollection";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_data_collection);
        Log.d(TAG, "Starting activity");

        // Set initial layout
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });

        // Launch service, no longer need activity
        Intent launchDataService = new Intent(this, WatchDataLayerListenerService.class);
        startService(launchDataService);
    }
}
