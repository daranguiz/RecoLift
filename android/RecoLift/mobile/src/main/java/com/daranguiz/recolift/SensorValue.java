package com.daranguiz.recolift;

import java.util.Vector;

/* Individual sensor measurements */
public class SensorValue {
    public SensorValue(long mTimestamp, float[] mValues) {
        timestamp = mTimestamp;
        values = mValues.clone();
    }

    public SensorValue(SensorValue old) {
        timestamp = old.timestamp;
        values = old.values.clone();
    }

    public long timestamp;
    public float[] values;
}
