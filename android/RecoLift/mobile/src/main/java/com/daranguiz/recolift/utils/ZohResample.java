package com.daranguiz.recolift.utils;

import com.daranguiz.recolift.datatype.SensorValue;

public class ZohResample {
    public ZohResample(SensorValue initialSensorValue, long inputSamplingDeltaNs) {
        samplingDeltaNs = inputSamplingDeltaNs;
        lastSensorValue = initialSensorValue;
    }

    private SensorValue lastSensorValue;
    private static long samplingDeltaNs;

    public SensorValue resample(SensorValue curSensorValue) {
        SensorValue returnValue;

        /* First sample is wonky, just avoid altogether */
        long nextSampleTime = lastSensorValue.timestamp + samplingDeltaNs;
        if (lastSensorValue.timestamp == -1) {
            returnValue = new SensorValue(curSensorValue);
        } else {
            /* If new sample happens before it's expected, take the old sample */
            if (curSensorValue.timestamp < nextSampleTime) {
                returnValue = new SensorValue(nextSampleTime, lastSensorValue.values);
            } else {
                returnValue = new SensorValue(nextSampleTime, curSensorValue.values);
            }
        }

        /* Update to save state */
        lastSensorValue = new SensorValue(nextSampleTime, curSensorValue.values);

        return returnValue;
    }
}
