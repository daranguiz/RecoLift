package com.daranguiz.recolift;

import java.util.Vector;

/* Class to hold all sensor measurements */
public class SensorData {
    public SensorData() {
        accel = new Vector<SensorValue>();
        gyro = new Vector<SensorValue>();
    }

    public Vector<SensorValue> accel;
    public Vector<SensorValue> gyro;

}
