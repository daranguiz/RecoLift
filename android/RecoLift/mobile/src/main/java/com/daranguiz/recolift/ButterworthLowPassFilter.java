package com.daranguiz.recolift;

import java.util.List;
import java.util.Vector;

public class ButterworthLowPassFilter {
    public ButterworthLowPassFilter() {
        pastXVals = new Vector<Float>(NUM_TAPS);
        pastYVals = new Vector<Float>(NUM_TAPS);
        for (int i = 0; i < NUM_TAPS; i++) {
            pastXVals.add(0f);
            pastYVals.add(0f);
        }
    }

    /* Filter coefficients, unity gain
     * See lpf_generator.m, current cutoff is 12Hz with Fs = 25Hz
     */
    private static final float[] b = {0.044969f, 0.224845f, 0.449691f, 0.449691f, 0.224845f, 0.044969f};
    private static final float[] a = {1.000000f, -0.196887f, 0.646994f, -0.067122f, 0.057795f, -0.001770f};
    private static final int NUM_TAPS = 5;

    /* State variables */
    private List<Float> pastXVals;
    private List<Float> pastYVals;

    /* Single-value filtering given state.
     * y[n] = (b0 x[n] + b1 x[n-1] + ...) - (a1 y[n-1] + a2 y[n-2] + ...)
     * NOTE: pastVals are -1 on index, ie pastXVals[0] refers to x[n-1]
     */
    public float singleFilt(float inputVal) {
        float outputVal = b[0] * inputVal;
        for (int i = 0; i < NUM_TAPS; i++) {
            outputVal += b[i+1] * pastXVals.get(i);
            outputVal -= a[i+1] * pastYVals.get(i);
        }

        /* Pop old vals, push new vals */
        pastXVals.remove(NUM_TAPS-1);
        pastYVals.remove(NUM_TAPS-1);
        pastXVals.add(0, inputVal);
        pastYVals.add(0, outputVal);

        return outputVal;
    }
}
