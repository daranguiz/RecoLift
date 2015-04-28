package com.daranguiz.recolift.utils;

import android.util.Log;

import com.daranguiz.recolift.datatype.SegmentationFeatures;
import com.daranguiz.recolift.datatype.SensorType;
import com.daranguiz.recolift.datatype.SensorValue;
import com.daranguiz.recolift.utils.RecoFileUtils;
import com.daranguiz.recolift.utils.RecoMath;

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
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import Jama.Matrix;

// TODO: Remove openCV, going to spin my own PCA instead
public class SegmentationPhase {
    public SegmentationPhase(Map<SensorType, List<SensorValue>> sensorDataRef) {
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
    private Map<SensorType, List<SensorValue>> mSensorData;
    private RecoMath mRecoMath;
    private static final SensorType[] sensorTypeCache = SensorType.values();

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
        /* Only get buffer when all sensor sources have enough */
        while (isBufferAvailable()) {
            /* Get current sliding window buffer */
            Map<SensorType, List<SensorValue>> bufferAsSensorData = getNextBuffer();

            Map<SensorType, List<SegmentationFeatures>> bufferSegmentationFeatures = new TreeMap<>();
            for (SensorType sensor : sensorTypeCache) {
                bufferSegmentationFeatures.put(sensor, new Vector<SegmentationFeatures>());
                double[][] buffer = bufferToDoubleArray(bufferAsSensorData, sensor);

                /* Condense axes to single principal component */
                Matrix firstPrincipalComponent = mRecoMath.computePCA(buffer, NUM_DOFS, WINDOW_SIZE);
                double[] primaryProjection = mRecoMath.projectPCA(buffer, firstPrincipalComponent);

                /* Compute magnitude */
                double[] signalMagnitude = mRecoMath.computeSignalMagnitude(buffer, NUM_DOFS, WINDOW_SIZE);

                /* Compute features */
                bufferSegmentationFeatures.get(sensor).add(computeSegmentationFeatures(primaryProjection));
                bufferSegmentationFeatures.get(sensor).add(computeSegmentationFeatures(signalMagnitude));
                for (int i = 0; i < NUM_DOFS; i++) {
                    bufferSegmentationFeatures.get(sensor).add(computeSegmentationFeatures(buffer[i]));
                }
            }

            /* Log features to CSV */
            if (!isFirstLogging) {
                long firstValueTimestamp = bufferAsSensorData.get(sensorTypeCache[0]).get(0).timestamp;
                logSegmentationFeatures(bufferSegmentationFeatures, firstValueTimestamp);
            } else {
                // Timestamp is messed up for first buffer for some reason, so ignore first
                isFirstLogging = false;
            }

            // TODO: Pass features into classifier

            // TODO: Accumulator
        }
    }

    /* Check if there are enough samples for a new buffer */
    private boolean isBufferAvailable() {
        int nextBufferStart = bufferPointer + SLIDE_AMOUNT;
        int nextBufferEnd = nextBufferStart + WINDOW_SIZE;
        boolean retVal = true;

        /* If any sensor streams don't have a full buffer, don't process */
        for (SensorType sensor : sensorTypeCache) {
            if (nextBufferEnd >= mSensorData.get(sensor).size()) {
                retVal = false;
            }
        }

        return retVal;
    }

    /* Create a new SensorData instance with just the buffer of interest.
     * Right now, this only accounts for single-source accelerometer data, but it'll be
     * extensible shortly.
     */
    private Map<SensorType, List<SensorValue>> getNextBuffer() {
        Map<SensorType, List<SensorValue>> buffer = new TreeMap<>();
        int nextBufferPointer = bufferPointer + WINDOW_SIZE;

        /* List is implemented as a vector */
        for (SensorType sensor : sensorTypeCache) {
            List<SensorValue> newSubList = mSensorData.get(sensor).subList(bufferPointer, nextBufferPointer);
            buffer.put(sensor, newSubList);
        }
        bufferPointer = bufferPointer + SLIDE_AMOUNT;

        return buffer;
    }

    /* Convert our SensorValue buffer to a float array for easy computation */
    private double[][] bufferToDoubleArray(Map<SensorType, List<SensorValue>> buffer, SensorType sensor) {
        double outputArr[][] = new double[NUM_DOFS][WINDOW_SIZE];

        /* No easy way to do quick array copies in current form */
        for (int i = 0; i < NUM_DOFS; i++) {
            for (int j = 0; j < WINDOW_SIZE; j++) {
                outputArr[i][j] = (double) buffer.get(sensor).get(j).values[i];
            }
        }

        return outputArr;
    }

    /* Compute segmentation features */
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
    // TODO: Change to accept Map<> and iterate over all features, one line
    private void logSegmentationFeatures(Map<SensorType, List<SegmentationFeatures>> bufferSegmentationFeatures, long timestamp) {
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

        for (SensorType sensor : sensorTypeCache) {
            for (SegmentationFeatures features : bufferSegmentationFeatures.get(sensor)) {
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
                csvLine += df.format(features.secondHalfVariance) + ", ";
            }
        }

        /* Remove the final comma */
        csvLine = csvLine.substring(0, csvLine.length() - 2);

        /* Write */
        writer.println(csvLine);

        /* Close PrintWriter every time */
        writer.close();
    }

}
