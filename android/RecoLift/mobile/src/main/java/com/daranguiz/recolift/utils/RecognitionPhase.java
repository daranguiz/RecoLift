package com.daranguiz.recolift.utils;

import android.util.Log;

import com.daranguiz.recolift.datatype.RecognitionFeatures;
import com.daranguiz.recolift.datatype.SensorType;
import com.daranguiz.recolift.datatype.SensorValue;

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
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.SparseInstance;

public class RecognitionPhase {
    public RecognitionPhase(Map<SensorType, List<SensorValue>> sensorDataRef) {
        bufferPointer = 0;
        mSensorData = sensorDataRef;
        mRecoMath = new RecoMath();

        /* Get file pointer, don't open yet */
        String timestamp = new SimpleDateFormat("yyy.MM.dd.HH.mm.ss").format(new Date());
        String filename = timestamp + "_recognition_features.csv";
        csvFile = new RecoFileUtils().getFileInDcimStorage(filename);
        isFirstLogging = true;

        initClassifier();
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
    private static final SensorType[] sensorTypeCache = SensorType.values();

    /* Buffer */
    private int bufferPointer;
    private Map<SensorType, List<SensorValue>> mSensorData;
    private RecoMath mRecoMath;

    /* Classification */
    private Classifier recognitionSvm;
    private static final String recognitionSvmModelFilename = "RecoLiftRecognitionSVM.model";
    private static final int NUM_ATTS = 201;
    private Instances svmDataset;

    /* Logging */
    private static File csvFile;
    private static boolean isFirstLogging;

    /* Run full recognition window in one batch */
    public void performBatchRecognition(int startIdx, int endIdx) {
        if (!collectGroundTruth) {
            bufferPointer = startIdx;
        }

        /* Only get buffer when all sensor sources have enough */
        while (isBufferAvailable(endIdx)) {
            /* Get current sliding window buffer */
            Map<SensorType, List<SensorValue>> bufferAsSensorData = getNextBuffer();

            // TODO: Which features am I going to use?
            Map<SensorType, List<RecognitionFeatures>> bufferRecognitionFeatures = new TreeMap<>();
            for (SensorType sensor : sensorTypeCache) {
                bufferRecognitionFeatures.put(sensor, new Vector<RecognitionFeatures>());
                double[][] buffer = bufferToDoubleArray(bufferAsSensorData, sensor);

                /* Condense axes to single principal component */
                Matrix firstPrincipalComponent = mRecoMath.computePCA(buffer, NUM_DOFS, WINDOW_SIZE);
                double[] primaryProjection = mRecoMath.projectPCA(buffer, firstPrincipalComponent);

                /* Compute magnitude */
                double[] signalMagnitude = mRecoMath.computeSignalMagnitude(buffer, NUM_DOFS, WINDOW_SIZE);

                /* Compute features */
                bufferRecognitionFeatures.get(sensor).add(computeRecognitionFeatures(primaryProjection));
                bufferRecognitionFeatures.get(sensor).add(computeRecognitionFeatures(signalMagnitude));
                for (int i = 0; i < NUM_DOFS; i++) {
                    bufferRecognitionFeatures.get(sensor).add(computeRecognitionFeatures(buffer[i]));
                }
            }

            /* Log features to CSV */
            if (!isFirstLogging) {
                long firstValueTimestamp = bufferAsSensorData.get(sensorTypeCache[0]).get(0).timestamp;
                logRecognitionFeatures(bufferRecognitionFeatures, firstValueTimestamp);
            } else {
                // Timestamp is messed up for the first buffer for some reason, so ignore first
                isFirstLogging = false;
            }

            // TODO: Pass features into classifier
//            performRecognitionClassification(bufferRecognitionFeatures);

            // TODO: Accumulator
        }
    }

    /* Check if there are enough samples for a new buffer */
    // TODO: Refactor? Very similar to SegmentationPhase
    private boolean isBufferAvailable(int endIdx) {
        boolean retVal = true;
        int nextBufferStart = bufferPointer + SLIDE_AMOUNT;
        int nextBufferEnd = nextBufferStart + WINDOW_SIZE;

        /* Only go up to the end of the noted exercise window */
        if (collectGroundTruth) {
            for (SensorType sensor : sensorTypeCache) {
                if (nextBufferEnd >= mSensorData.get(sensor).size()) {
                    retVal = false;
                }
            }
        } else {
            if (nextBufferEnd >= endIdx) {
                retVal = false;
            }
        }

        return retVal;
    }

    /* Create a new SensorData instance with just the buffer of interest. */
    // TODO: Refactor, is same as SegmentationPhase.getNextBuffer()
    private Map<SensorType, List<SensorValue>> getNextBuffer() {
        Map<SensorType, List<SensorValue>> buffer = new TreeMap<>();
        int nextBufferPointer = bufferPointer + WINDOW_SIZE;

        for (SensorType sensor : sensorTypeCache) {
            List<SensorValue> newSublist = mSensorData.get(sensor).subList(bufferPointer, nextBufferPointer);
            buffer.put(sensor, newSublist);
        }
        bufferPointer = bufferPointer + SLIDE_AMOUNT;

        return buffer;
    }

    /* Convert SensorValue buffer to a float array for easier computation */
    // TODO: Refactor
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
    private void logRecognitionFeatures(Map<SensorType, List<RecognitionFeatures>> bufferRecognitionFeatures, long timestamp) {
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

        for (SensorType sensor : sensorTypeCache) {
            for (RecognitionFeatures features : bufferRecognitionFeatures.get(sensor)) {
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
            }
        }

        /* Remove the final comma */
        csvLine = csvLine.substring(0, csvLine.length() - 2);

        /* Write */
        writer.println(csvLine);

        /* Close every time so there are no dangling file handles */
        writer.close();
    }

    private int performRecognitionClassification(Map<SensorType, List<RecognitionFeatures>> featureMap) {
        /* WEKA input works with double array */
        double[] features = serializeRecognitionFeaturesMap(featureMap);

        /* Create WEKA instance to pass into classifier */
        int weight = 1;
        Instance recognitionInstance = new SparseInstance(weight, features);
        recognitionInstance.setDataset(svmDataset);

        double classificationResultDouble = -8008;
        try {
            classificationResultDouble = recognitionSvm.classifyInstance(recognitionInstance);
        } catch(Exception e) {
            Log.e(TAG, "Error, recognition classification failed! " + e.getLocalizedMessage());
        }

        Log.d(TAG, "Classification result: " + classificationResultDouble);

        return (int)classificationResultDouble;
    }

    private double[] serializeRecognitionFeaturesMap(Map<SensorType, List<RecognitionFeatures>> featureMap) {
        double[] serializedFeatures;

        List<Double> featureList = new Vector<>();
        for (SensorType sensor : sensorTypeCache) {
            for (RecognitionFeatures features : featureMap.get(sensor)) {
                for (int i = 0; i < RecognitionFeatures.NUM_AUTOC_BINS; i++) {
                    featureList.add((double)features.autocBins[i]);
                }
                featureList.add((double)features.rms);
                for (int i = 0; i < RecognitionFeatures.NUM_POWER_BAND_BINS; i++) {
                    featureList.add((double)features.powerBandMagnitudes[i]);
                }
                featureList.add((double)features.mean);
                featureList.add((double)features.stdDev);
                featureList.add((double)features.kurtosis);
                featureList.add((double)features.interquartileRange);
            }
        }

        serializedFeatures = new double[featureList.size()];
        for (int i = 0; i < featureList.size(); i++) {
            serializedFeatures[i] = featureList.get(i);
        }

        return serializedFeatures;
    }

    private void initClassifier() {
        try {
            File modelFullPath = new RecoFileUtils().getFileInDcimStorage(recognitionSvmModelFilename);
            recognitionSvm = (Classifier) SerializationHelper.read(modelFullPath.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Recognition SVM could not be loaded");
        }

        /* Init attributes */
        FastVector atts = new FastVector();
        FastVector classVal = new FastVector();

        // TODO: Relate this to actual class values
        classVal.addElement("Lift1");
        classVal.addElement("Lift2");

        for (int i = 0; i < NUM_ATTS-1; i++) {
            atts.addElement(new Attribute("attribute_" + Integer.toString(i)));
        }
        atts.addElement(new Attribute("attribute_" + NUM_ATTS, classVal));

        /* Set! */
        svmDataset = new Instances("RecognitionInstances", atts, 0);
        svmDataset.setClassIndex(svmDataset.numAttributes() - 1);
    }
}
