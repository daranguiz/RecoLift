package com.daranguiz.recolift.datatype;

public class RecognitionFeatures {
    public RecognitionFeatures() {
        autocBins = new double[NUM_AUTOC_BINS];
        powerBandMagnitudes = new double[NUM_POWER_BAND_BINS];
    }

    /* Constants */
    public static final int NUM_AUTOC_BINS = 5;
    public static final int NUM_POWER_BAND_BINS = 10;

    /* Autoc-related features */
    public double[] autocBins;

    /* Energy-related features */
    public double rms;
    public double[] powerBandMagnitudes;

    /* Statistical features */
    public double mean;
    public double stdDev;
    public double kurtosis;
    public double interquartileRange;
}
