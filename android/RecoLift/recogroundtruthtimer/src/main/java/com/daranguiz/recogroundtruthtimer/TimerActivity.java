package com.daranguiz.recogroundtruthtimer;

import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnItemSelected;


public class TimerActivity extends ActionBarActivity {
    /* View Injections */
    @InjectView(R.id.button_start_collection) Button buttonStartCollection;
    @InjectView(R.id.button_stop_collection) Button buttonStopCollection;
    @InjectView(R.id.button_start_lift) Button buttonStartLift;
    @InjectView(R.id.button_stop_lift) Button buttonStopLift;
    @InjectView(R.id.text_collection_status) TextView textCollectionStatus;
    @InjectView(R.id.text_lift_status) TextView textLiftStatus;
    @InjectView(R.id.spinner_lift_selection) Spinner spinnerLiftSelection;

    /* Constants */
    private static final String TAG = "TimerActivity";

    /* Logging */
    private static File csvFile;
    private static String curLift;

    /* Lift Options */
    private static final String[] liftArray = {
            "SelectLift",
            "LowBarSquat",
            "HighBarSquat",
            "Bench",
            "PendlayRow",
            "OverheadPress",
            "ConventionalDeadlift",
            "EZCurl",
            "PreacherCurl",
            "Skullcrusher",
            "SeatedRow",
            "TricepPushdown",
            "Pullup"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        ButterKnife.inject(this);
        curLift = "NoLiftSelected";

        /* Spinner init */
        ArrayAdapter<String> adapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, liftArray);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinnerLiftSelection.setAdapter(adapter);

        /* Spinner listener */
        spinnerLiftSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                curLift = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Lift selected: " + curLift);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "No lifts selected");
            }
        });
    }

    /* Button Handlers */
    @OnClick(R.id.button_start_collection)
    public void startCSV() {
        Log.d(TAG, "Starting collection");

        /* Get file pointer */
        String timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        String filename = timestamp + "_ground_truth.csv";
        csvFile = getFileInDcimStorage(filename);

        /* Open a new PrintWriter every time to avoid unused open file descriptors */
        PrintWriter writer;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(csvFile, true)));
        } catch (IOException e) {
            Log.d(TAG, "Could not open file for recording ground truth");
            return;
        }

        /* Write start time */
        String newLine = "";
        newLine += System.currentTimeMillis() + ", ";
        newLine += "StartCollection";

        /* End csv write */
        writer.println(newLine);
        writer.close();

        /* Update UI */
        textCollectionStatus.setText("Collection Started");
    }

    @OnClick(R.id.button_stop_collection)
    public void stopCSV() {
        Log.d(TAG, "Stopping collection");

        /* Open a new PrintWriter every time to avoid unused open file descriptors */
        PrintWriter writer;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(csvFile, true)));
        } catch (IOException e) {
            Log.d(TAG, "Could not open file for recording ground truth");
            return;
        }

        /* Write start time */
        String newLine = "";
        newLine += System.currentTimeMillis() + ", ";
        newLine += "StopCollection";

        /* End */
        writer.println(newLine);
        writer.close();

        /* Update UI */
        textCollectionStatus.setText("Collection Stopped");
    }

    @OnClick(R.id.button_start_lift)
    public void startLift() {
        Log.d(TAG, "Starting lift: " + curLift);

        /* Open a new PrintWriter every time to avoid unused open file descriptors */
        PrintWriter writer;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(csvFile, true)));
        } catch (IOException e) {
            Log.d(TAG, "Could not open file for recording ground truth");
            return;
        }

        /* Write start time */
        String newLine = "";
        newLine += System.currentTimeMillis() + ", ";
        newLine += "StartLift, ";
        newLine += curLift;

        /* End */
        writer.println(newLine);
        writer.close();

        /* Update UI */
        textLiftStatus.setText("Lift Started");
    }

    @OnClick(R.id.button_stop_lift)
    public void stopLift() {
        Log.d(TAG, "Stopping lift: " + curLift);

        /* Open a new PrintWriter every time to avoid unused open file descriptors */
        PrintWriter writer;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(csvFile, true)));
        } catch (IOException e) {
            Log.d(TAG, "Could not open file for recording ground truth");
            return;
        }

        /* Write start time */
        String newLine = "";
        newLine += System.currentTimeMillis() + ", ";
        newLine += "StopLift";

        /* End */
        writer.println(newLine);
        writer.close();

        /* Update UI */
        textLiftStatus.setText("Lift Stopped");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_timer, menu);
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

    /* Get handle to file in public pictures directory */
    public File getFileInDcimStorage(String filename) {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), filename);
        return file;
    }
}
