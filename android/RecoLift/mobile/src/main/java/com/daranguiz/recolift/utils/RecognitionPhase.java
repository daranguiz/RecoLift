package com.daranguiz.recolift.utils;

import android.util.Log;

import com.daranguiz.recolift.datatype.RecognitionFeatures;
import com.daranguiz.recolift.datatype.SensorData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import Jama.Matrix;

public class RecognitionPhase {
    public RecognitionPhase(SensorData sensorDataRef) {
        bufferPointer = 0;
        mSensorData = sensorDataRef;
        mRecoMath = new RecoMath();

        /* Get file pointer, don't open yet */
        String timestamp = new SimpleDateFormat("yyy.MM.dd.HH.mm.ss").format(new Date());
        String filename = timestamp + "_recognition_features.csv";
        csvFile = new RecoFileUtils().getFileInDcimStorage(filename);
    }

    /* Constants */
    private static final int F_S = 25;
    private static final int HALF_SEC_DELAY = F_S / 2;
    private static final int WINDOW_SIZE = F_S * 5;
    private static final int SLIDE_AMOUNT = 5;
    private static final int NUM_DOFS = 3;
    private static final int NUM_SIDE_PEAK = 2;
    private static final String TAG = "RecognitionPhase";
    private static final boolean collectGroundTruth = true;

    /* Buffer */
    private int bufferPointer;
    private SensorData mSensorData;
    private RecoMath mRecoMath;

    /* Logging */
    private static File csvFile;

    /* Run full recognition window in one batch */
    public void performBatchRecognition(int startIdx, int endIdx) {
        if (!collectGroundTruth) {
            bufferPointer = startIdx;
        }

        while (isBufferAvailable(endIdx)) {
            /* Get current sliding window buffer */
            SensorData bufferAsSensorData = getNextBuffer();
            double[][] buffer = bufferToDoubleArray(bufferAsSensorData);

            /* Condense axes to single principal component */
            Matrix firstPrincipalComponent = mRecoMath.computePCA(buffer, NUM_DOFS, WINDOW_SIZE);
            double[] primaryProjection = mRecoMath.projectPCA(buffer, firstPrincipalComponent);

            /* Compute features on primaryProjection */
            RecognitionFeatures signalFeatures = computeRecognitionFeatures(primaryProjection);

            /* Log features to CSV */
            long firstValueTimestamp = bufferAsSensorData.accel.get(0).timestamp;
            logRecognitionFeatures(signalFeatures, firstValueTimestamp, 0);

            // TODO: Pass features into classifier

            // TODO: Accumulator
        }
    }

    /* Check if there are enough samples for a new buffer */
    // TODO: Generalize to any source
    // TODO: Refactor? Very similar to SegmentationPhase
    private boolean isBufferAvailable(int endIdx) {
        boolean retVal = true;
        int nextBufferStart = bufferPointer + SLIDE_AMOUNT;
        int nextBufferEnd = nextBufferStart + WINDOW_SIZE;

        /* Only go up to the end of the noted exercise window */
        if (collectGroundTruth) {
            if (nextBufferEnd >= mSensorData.accel.size()) {
                retVal = false;
            }
        } else {
            if (nextBufferEnd >= endIdx) {
                retVal = false;
            }
        }

        return retVal;
    }

    /* Create a new SensorData instance with just the buffer of interest. */
    // TODO: Generalize to any source
    // TODO: Refactor
    private SensorData getNextBuffer() {
        SensorData buffer = new SensorData();
        int nextBufferPointer = bufferPointer + WINDOW_SIZE;

        buffer.accel = mSensorData.accel.subList(bufferPointer, nextBufferPointer);
        bufferPointer = bufferPointer + SLIDE_AMOUNT;

        return buffer;
    }

    /* Convert SensorValue buffer to a float array for easier computation */
    // TODO: Generalize to any source
    // TODO: Refactor
    private double[][] bufferToDoubleArray(SensorData buffer) {
        double outputArr[][] = new double[NUM_DOFS][WINDOW_SIZE];

        /* No easy way to do quick array copies in current form */
        for (int i = 0; i < NUM_DOFS; i++) {
            for (int j = 0; j < WINDOW_SIZE; j++) {
                outputArr[i][j] = (double) buffer.accel.get(j).values[i];
            }
        }

        return outputArr;
    }

    /* Compute recognition features */
    private RecognitionFeatures computeRecognitionFeatures(double[] signal) {
        RecognitionFeatures curRecognitionFeatures = new RecognitionFeatures();

        /* Autoc features */
        double[] autoc = mRecoMath.computeAutocorrelation(signal);
        curRecognitionFeatures.autocBins = mRecoMath.computeBinnedSignalSum(autoc, RecognitionFeatures.NUM_AUTOC_BINS);

        /* Energy features */
        curRecognitionFeatures.rms = mRecoMath.computeRms(signal, 0, signal.length);
        curRecognitionFeatures.powerBandMagnitudes = mRecoMath.computePowerBandSums(signal, RecognitionFeatures.NUM_POWER_BAND_BINS);

        /* Statistical features */
        curRecognitionFeatures.mean = mRecoMath.computeMean(signal, 0, signal.length);
        curRecognitionFeatures.stdDev = mRecoMath.computeStdDev(signal, 0, signal.length);
        curRecognitionFeatures.kurtosis = mRecoMath.computeFourthStandardizedMoment(signal, 0, signal.length);
        curRecognitionFeatures.interquartileRange = mRecoMath.computeInterquartileRange(signal);

        return curRecognitionFeatures;
    }

    /* Log recognition features to file */
    private void logRecognitionFeatures(RecognitionFeatures features, long timestamp, int sensorType) {
        PrintWriter writer;

        /* Open a new PrintWriter every time to avoid unused open file descriptors
         * when the process ends.
         */
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(csvFile, true)));
        } catch (IOException e) {
            Log.d(TAG, "Could not open file for writing recognition features");
            return;
        }

        /* Doubles print in scientific notation without this, gross but necessary */
        DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        df.setMaximumFractionDigits(340);

        /* Construct feature string... gross but deal with it */
        String csvLine = "";
        csvLine += timestamp + ", ";
        csvLine += sensorType + ", ";

        /* Autoc features */
        for (int i = 0; i < RecognitionFeatures.NUM_AUTOC_BINS; i++) {
            csvLine += df.format(features.autocBins[i]) + ", ";
        }

        /* Energy features */
        csvLine += df.format(features.rms) + ", ";
        for (int i = 0; i < RecognitionFeatures.NUM_POWER_BAND_BINS; i++) {
            csvLine += df.format(features.powerBandMagnitudes[i]) + ", ";
        }

        /* Statistical features */
        csvLine += df.format(features.mean) + ", ";
        csvLine += df.format(features.stdDev) + ", ";
        csvLine += df.format(features.kurtosis) + ", ";
        csvLine += df.format(features.interquartileRange) + ", ";

        /* Write */
        writer.println(csvLine);

        /* Close every time so there are no dangling file handles */
        writer.close();
    }
}
