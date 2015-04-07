package com.daranguiz.recolift.datatype;

/* Container for segmentation phase features on a per-window basis */
public class SegmentationFeatures {
    public SegmentationFeatures() {
        powerBandMagnitudes = new double[NUM_POWER_BAND_BINS];
    }

    /* Constants */
    public static final int NUM_POWER_BAND_BINS = 10;

    /* Autoc-related features */
    public int numAutocPeaks;
    public int numProminentAutocPeaks;
    public int numWeakAutocPeaks;
    public double maxAutocPeakValue;
    public double firstAutocPeakValue;
    public boolean firstAndMaxPeakValuesEqual;


    /* Energy-related features */
    public double fullRms;
    public double firstHalfRms;
    public double secondHalfRms;
    public double cusumRms;
    public double[] powerBandMagnitudes;


    /* Statistical features */
    public double fullMean;
    public double firstHalfMean;
    public double secondHalfMean;

    public double fullStdDev;
    public double firstHalfStdDev;
    public double secondHalfStdDev;

    public double fullVariance;
    public double firstHalfVariance;
    public double secondHalfVariance;
}
