package com.daranguiz.recolift.datatype;

import java.util.List;
import java.util.Vector;

/* Class to hold all sensor measurements */
public class SensorData {
    public SensorData() {
        accel = new Vector<SensorValue>();
        gyro = new Vector<SensorValue>();
    }

    public List<SensorValue> accel;
    public List<SensorValue> gyro;

}
