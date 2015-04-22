package com.daranguiz.recolift.utils;

import com.daranguiz.recolift.datatype.SensorData;

import java.io.File;

public class CountingPhase {
    public CountingPhase(SensorData sensorDataRef) {

    }

    /* Constants */
    private static final String TAG = "CountingPhase";

    /* Sensor */
    private SensorData mSensorData;

    /* Logging */
    private static RecoFileUtils mRecoFileUtils;
    private static File csvFile;

    /* Thoughts!
     * So with my approach, I don't think I really need to worry about doing it 100%
     * online. Like, I could wait until a full exercise has completed then do counting
     * across the whole buffer. I think that'll be easier.
     *
     * From python simulation, algorithm:
     * 1:
     *  - Find the local maxima
     *  - Sort by amplitude
     *  - Add to candidate peak set if distance to any peak already in the set is at
     *      least minPeriod away
     *
     * 2:
     * For each candidate peak:
     *  - Perform autoc around peak, find maximum value. This gives local period P
     *  - Loop through candidate peaks inside window
     *      > Remove from set if <0.75P away from any other peak
     *
     * 3:
     *  - Find peak at 40th percentile
     *  - Reject any peaks smaller than half the amplitude of that peak
     *  - Return number of remaining peaks as final value
     */
}
