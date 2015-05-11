package com.daranguiz.recolift;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class LandingActivity extends ActionBarActivity {

    /* Constants */
    public static final String GROUND_TRUTH_CHECKBOX = "GROUND_TRUTH_CHECKBOX";

    @InjectView(R.id.button_begin_workout) Button beginWorkoutButton;
    @InjectView(R.id.checkBox_collect_ground_truth) CheckBox collectGroundTruthCheckbox;

    @OnClick(R.id.button_begin_workout)
    public void beginWorkout() {
        beginWorkoutButton.setText("WORKOUT STARTED");

        /* Start tracker activity */
        Intent intent = new Intent(this, TrackerActivity.class);
        boolean checkboxVal = collectGroundTruthCheckbox.isChecked();
        intent.putExtra(GROUND_TRUTH_CHECKBOX, checkboxVal);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        ButterKnife.inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        beginWorkoutButton.setText("Start workout");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_landing, menu);
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
}
