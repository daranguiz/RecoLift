package com.daranguiz.recolift.utils;

import android.util.Log;

import com.daranguiz.recolift.datatype.SegmentationFeatures;
import com.daranguiz.recolift.utils.RecoFileUtils;
import com.daranguiz.recolift.utils.RecoMath;
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
import java.util.List;
import java.util.Locale;

import Jama.Matrix;

// TODO: Remove openCV, going to spin my own PCA instead
public class SegmentationPhase {
    public SegmentationPhase(SensorData sensorDataRef) {
        bufferPointer = 0;
        mSensorData = sensorDataRef;
        isFirstLogging = true;
        mRecoMath = new RecoMath();
        mRecoFileUtils = new RecoFileUtils();

        /* Get file pointer, don't open yet */
        String timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        String filename = timestamp + "_segmentation_features.csv";
        csvFile = mRecoFileUtils.getFileInDcimStorage(filename);
    }

    /* Constants */
    private static final int F_S = 25;
    private static final int HALF_SEC_DELAY = F_S / 2;
    private static final int WINDOW_SIZE = F_S * 5;
    private static final int SLIDE_AMOUNT = 5;
    private static final int NUM_DOFS = 3;
    private static final int NUM_SIDE_PEAK = 2;
    private static final String TAG = "SegmentationPhase";

    private int bufferPointer;
    private SensorData mSensorData;
    private RecoMath mRecoMath;

    /* Logging */
    private static RecoFileUtils mRecoFileUtils;
    private static File csvFile;
    private static boolean isFirstLogging; // hack

    /* Okay, talking myself through on this one.
       I have a large vector of data that i'll have to grab a sliding window over.
       Say I have... 6 seconds of data. With sliding windows of 5-sec a piece and 0.2s slide,
       I'll have 5 total windows. Ideally, I'll have a function that takes all my unused samples and
       splits them into multiple buffers of length 5s, with the proper sliding window split.

       Buffer function will keep track of how far into the vector it made. There is going to have to
       be some sort of garbage collection, ie if no exercise is detected over a 10 second span
       (determined by the aggregator), the last 5 seconds (unused in any subsequent buffers)
       is deleted from the vector. NOTE: This must be a synchronous operation.

       Once I have my buffer to work with, it should be a bit easier. Compute features, run through
       classifier.

       NOTE: Synchronizing phone resampling timestamps with watch resampling timestamps
       may be non-trivial. Think on this.

       Aggregator thoughts: Paper says to increment on +exercise, decrement on -exercise. Flag as
       exercise when six seconds of +exercise (at 0.2sec per, == 30). If there is ANY exercise
       occurring in the window, it should be flagged. *matters on the ground truth input.
     */
    // TODO: Refactor into: Get buffer -> Get axes -> Get features -> Classify -> Accumulate
    public void performBatchSegmentation() {
        while (isBufferAvailable()) {
            /* Get current sliding window buffer */
            SensorData bufferAsSensorData = getNextBuffer();
            double[][] buffer = bufferToDoubleArray(bufferAsSensorData);

            /* Condense axes to single principal component */
            Matrix firstPrincipalComponent = mRecoMath.computePCA(buffer, NUM_DOFS, WINDOW_SIZE);
            double[] primaryProjection = mRecoMath.projectPCA(buffer, firstPrincipalComponent);

            /* Compute features on primaryProjection */
            SegmentationFeatures signalFeatures = computeSegmentationFeatures(primaryProjection);

            /* Log features to CSV */
            if (!isFirstLogging) {
                long firstValueTimestamp = bufferAsSensorData.accel.get(0).timestamp;
                logSegmentationFeatures(signalFeatures, firstValueTimestamp, 0);
            } else {
                // Timestamp is messed up for first buffer for some reason, so ignore first
                isFirstLogging = false;
            }

            // TODO: Pass features into classifier

            // TODO: Accumulator
        }
    }

    /* Check if there are enough samples for a new buffer */
    // TODO: Generalize to any source
    private boolean isBufferAvailable() {
        int nextBufferStart = bufferPointer + SLIDE_AMOUNT;
        int nextBufferEnd = nextBufferStart + WINDOW_SIZE;
        boolean retVal = true;

        /* Corner case, size() == idx? */
        if (nextBufferEnd >= mSensorData.accel.size()) {
            retVal = false;
        }

        return retVal;
    }

    /* Create a new SensorData instance with just the buffer of interest.
     * Right now, this only accounts for single-source accelerometer data, but it'll be
     * extensible shortly.
     */
    // TODO: Generalize to any source
    private SensorData getNextBuffer() {
        SensorData buffer = new SensorData();
        int nextBufferPointer = bufferPointer + WINDOW_SIZE;

        /* List is implemented as a vector */
        buffer.accel = mSensorData.accel.subList(bufferPointer, nextBufferPointer);
        bufferPointer = bufferPointer + SLIDE_AMOUNT;

        return buffer;
    }

    // TODO: SensorValue is poorly optimized for quick array copies. Check if timing met.
    /* Convert our SensorValue buffer to a float array for easy computation */
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

    /* Compute segmentation features */
    // TODO: Check for const correctness on inputs
    private static final int NUM_POWER_BAND_BINS = 10;
    private SegmentationFeatures computeSegmentationFeatures(double[] signal) {
        SegmentationFeatures curSegmentationFeatures = new SegmentationFeatures();

        /* Autoc features */
        double[] autoc = mRecoMath.computeAutocorrelation(signal);
        List<Integer> autocPeaks = mRecoMath.computePeakIndices(autoc, NUM_SIDE_PEAK, HALF_SEC_DELAY);
        curSegmentationFeatures.numAutocPeaks              = autocPeaks.size();
        curSegmentationFeatures.numProminentAutocPeaks     = mRecoMath.computeNumProminentPeaks(autoc, autocPeaks);
        curSegmentationFeatures.numWeakAutocPeaks          = mRecoMath.computeNumWeakPeaks(autoc, autocPeaks);
        curSegmentationFeatures.maxAutocPeakValue          = mRecoMath.findMaxPeakValue(autoc, autocPeaks);
        curSegmentationFeatures.firstAutocPeakValue        = mRecoMath.findFirstPeakValueAfterZc(autoc, autocPeaks);
        curSegmentationFeatures.firstAndMaxPeakValuesEqual = curSegmentationFeatures.maxAutocPeakValue == curSegmentationFeatures.firstAutocPeakValue;

        /* Energy features */
        curSegmentationFeatures.fullRms             = mRecoMath.computeRms(signal, 0, signal.length);
        curSegmentationFeatures.firstHalfRms        = mRecoMath.computeRms(signal, 0, signal.length/2);
        curSegmentationFeatures.secondHalfRms       = mRecoMath.computeRms(signal, signal.length/2, signal.length/2);
        curSegmentationFeatures.cusumRms            = mRecoMath.computeRms(mRecoMath.computeCusum(signal), 0, signal.length/2);
        curSegmentationFeatures.powerBandMagnitudes = mRecoMath.computePowerBandSums(signal, NUM_POWER_BAND_BINS);

        /* Statistical features */
        curSegmentationFeatures.fullMean           = mRecoMath.computeMean(signal, 0, signal.length);
        curSegmentationFeatures.firstHalfMean      = mRecoMath.computeMean(signal, 0, signal.length/2);
        curSegmentationFeatures.secondHalfMean     = mRecoMath.computeMean(signal, signal.length/2, signal.length/2);

        curSegmentationFeatures.fullVariance       = mRecoMath.computeVariance(signal, 0, signal.length);
        curSegmentationFeatures.firstHalfVariance  = mRecoMath.computeVariance(signal, 0, signal.length/2);
        curSegmentationFeatures.secondHalfVariance = mRecoMath.computeVariance(signal, 0, signal.length/2);

        curSegmentationFeatures.fullStdDev         = Math.sqrt(curSegmentationFeatures.fullVariance);
        curSegmentationFeatures.firstHalfStdDev    = Math.sqrt(curSegmentationFeatures.firstHalfVariance);
        curSegmentationFeatures.secondHalfStdDev   = Math.sqrt(curSegmentationFeatures.secondHalfVariance);

        return curSegmentationFeatures;
    }

    /* Log segmentation features to file */
    private void logSegmentationFeatures(SegmentationFeatures features, long timestamp, int sensorType) {
        PrintWriter writer;

        /* Open a new PrintWriter every time to avoid unused open file descriptors */
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(csvFile, true)));
        } catch (IOException e) {
            Log.d(TAG, "Could not open file for writing segmentation features, disabling logging");
            return;
        }

        /* Doubles print in scientific notation otherwise, gross but necessary */
        DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        df.setMaximumFractionDigits(340);

        /* Construct feature string... gross */
        String csvLine = "";
        csvLine += timestamp + ", ";
        csvLine += sensorType + ", ";

        csvLine += features.numAutocPeaks + ", ";
        csvLine += features.numProminentAutocPeaks + ", ";
        csvLine += features.numWeakAutocPeaks + ", ";
        csvLine += df.format(features.maxAutocPeakValue) + ", ";
        csvLine += df.format(features.firstAutocPeakValue) + ", ";
        int firstAndMaxPeakValuesEqual = features.firstAndMaxPeakValuesEqual ? 1 : 0;
        csvLine += firstAndMaxPeakValuesEqual + ", ";

        csvLine += df.format(features.fullRms) + ", ";
        csvLine += df.format(features.firstHalfRms) + ", ";
        csvLine += df.format(features.secondHalfRms) + ", ";
        csvLine += df.format(features.cusumRms) + ", ";

        for (int i = 0; i < features.powerBandMagnitudes.length; i++) {
            csvLine += df.format(features.powerBandMagnitudes[i]) + ", ";
        }

        csvLine += df.format(features.fullMean) + ", ";
        csvLine += df.format(features.firstHalfMean) + ", ";
        csvLine += df.format(features.secondHalfMean) + ", ";

        csvLine += df.format(features.fullStdDev) + ", ";
        csvLine += df.format(features.firstHalfStdDev) + ", ";
        csvLine += df.format(features.secondHalfStdDev) + ", ";

        csvLine += df.format(features.fullVariance) + ", ";
        csvLine += df.format(features.firstHalfVariance) + ", ";
        csvLine += df.format(features.secondHalfVariance);

        /* Write */
        writer.println(csvLine);

        /* Close PrintWriter every time */
        writer.close();
    }

}
