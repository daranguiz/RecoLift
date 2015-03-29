package com.daranguiz.recolift.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class RecoMath {
    public RecoMath() {
    }
    /* Compute PCA using Jama library
     * Return float array of first principal component projection
     */
    public Matrix computePCA(double[][] buffer, int NUM_DOFS, int WINDOW_SIZE) {
        // May have to transpose, should be NUM_DOFS columns and WINDOW_SIZE rows (?)
        Matrix inputMat = new Matrix(buffer);

        /* Subtract mean w.r.t. each pixel */
        double mean[] = new double[NUM_DOFS];
        for (int i = 0; i < NUM_DOFS; i++) {
            mean[i] = getArrayMean(buffer[i]);
        }
        double zeroMeanMat[][] = inputMat.getArray();
        for (int i = 0; i < NUM_DOFS; i++) {
            for (int j = 0; j < WINDOW_SIZE; j++) {
                zeroMeanMat[i][j] = zeroMeanMat[i][j] - mean[i];
            }
        }

        /* Compute the scatter matrix, should be 3x3 */
        Matrix scatterMat = inputMat.times(inputMat.transpose());

        /* Compute eigenvalues/vectors */
        EigenvalueDecomposition curEig = scatterMat.eig();
        Matrix eigVecs = curEig.getV();
        double[] eigVals = curEig.getRealEigenvalues();

        /* Take just the principal component */
        int bestEigVal = getArrayMaxIdx(eigVals);
        Matrix bestEigVec = eigVecs.getMatrix(0, NUM_DOFS-1, bestEigVal, bestEigVal); // Row idx[], col idx[]

        /* We're working with U vector... I don't think we need to change anything back?
         * U = [3x1], A = [3x125], U' A = [1x3][3x125] = [1x125]
         */

        return bestEigVec;
    }

    /* Project the buffer onto the first principal component
     * Y = U' A
     */
    public double[] projectPCA(double[][] buffer, Matrix firstEigVec) {
        Matrix proj = firstEigVec.transpose().times(new Matrix(buffer));
        return proj.getColumnPackedCopy();
    }

    /* Compute mean of an array */
    public double getArrayMean(double[] arr) {
        double mean = 0;

        for (int i = 0; i < arr.length; i++) {
            mean += arr[i];
        }
        mean /= arr.length;

        return mean;
    }

    /* Get index of max value in an array */
    public int getArrayMaxIdx(double[] arr) {
        int maxIdx = 0;
        double maxVal = arr[0];

        for (int i = 0; i < arr.length; i++) {
             if (arr[i] > maxVal) {
                 maxVal = arr[i];
                 maxIdx = i;
             }
        }

        return maxIdx;
    }

    /* Compute autocorrelation
     * Relevant: http://stackoverflow.com/questions/12239096/computing-autocorrelation-with-fft-using-jtransforms-library
     * http://dsp.stackexchange.com/questions/736/how-do-i-implement-cross-correlation-to-prove-two-audio-files-are-similar
     */
    public double[] computeAutocorrelation(double[] buffer) {
        int n = buffer.length;

        /* The FFT method actually computes a circular convolution, so zero pad */
        double[] tmpBuf = new double[2 * n];
        System.arraycopy(buffer, 0, tmpBuf, 0, n);
        buffer = tmpBuf;
        n *= 2;

        /* Autoc = IFFT(FFT * FFT) */
        double[] autoc = new double[n];
        DoubleFFT_1D fft = new DoubleFFT_1D(n);

        /* Compute DFT, store into buffer */
        fft.realForward(buffer);

        /* Clear DC to remove mean, leave variance */
        autoc[0] = 0;
        autoc[1] = sqr(buffer[1]);

        /* Set real part equal to magnitude squared, imaginary equal to 0 */
        for (int i = 2; i < n; i += 2) {
            autoc[i] = sqr(buffer[i]) + sqr(buffer[i+1]);
            autoc[i+1] = 0;
        }

        /* IFFT to recover autoc */
        DoubleFFT_1D ifft = new DoubleFFT_1D(n);
        ifft.realInverse(autoc, true);

        /* Normalize */
        for (int i = 1; i < n; i++) {
            autoc[i] /= autoc[0];
        }
        autoc[0] = 1;

        /* Symmetric across zero, slice */
        double[] finalAutoc = new double[n/2];
        System.arraycopy(autoc, 0, finalAutoc, 0, n/2);

        return finalAutoc;
    }

    /* Square a number */
    public double sqr(double x) {
        return x * x;
    }

    /* Peak detection, n samples on either side */
    // TODO: Ignoring last n samples currently. Does this affect output?
    public List<Integer> computePeakIndices(double[] signal, int n, int delay) {
        int arrayLength = 2 * n + 1;
        double[] subSignal = new double[arrayLength];
        List<Integer> peakIndices = new Vector<Integer>();

        /* May want to start search late, ie on autoc, so as to ignore initial zero-lag peak */
        if (n > delay) {
            delay = n;
        }

        /* Loop over all possible sub-arrays */
        for (int i = delay; i < signal.length - n; i++) {
            System.arraycopy(signal, i-n, subSignal, 0, arrayLength);
            if (getArrayMaxIdx(subSignal) == n) {
                peakIndices.add(n);
            }
        }

        return peakIndices;
    }

    /* Compute number of prominent peaks */
    // TODO: Copy python pseudocode over to comments
    private static final double PROMINENT_PEAK_HEIGHT_THRESHOLD = 0.4;
    private static final int PEAK_LAG_THRESHOLD = 10;
    private static final int NEIGHBOR_THRESHOLD = 20;
    public int computeNumProminentPeaks(double[] signal, List<Integer> peakIndices) {
        Set<Integer> peaksToRemove = new HashSet<Integer>();
        int numPeaks = peakIndices.size();
        int peak1 = 0;
        int peak2 = 0;

        /* Populate set with peaks to remove from list */
        for (int i = 0; i < numPeaks; i++) {
            for (int j = 0; j < numPeaks; j++) {
                peak1 = peakIndices.get(i);
                peak2 = peakIndices.get(j);

                /* If peaks are neighbors but not referring to the same peak */
                if (isNeighbor(peak1, peak2, NEIGHBOR_THRESHOLD) && i != j) {
                    /* If too close, reject */
                    if (Math.abs(peak1 - peak2) < PEAK_LAG_THRESHOLD) {
                        peaksToRemove.add(peak1);
                        peaksToRemove.add(peak2);
                    }

                    /* If one peak isn't greater than another by a threshold, reject */
                    // TODO: Think about this, may not be correct
                    if (Math.abs(signal[peak1] - signal[peak2]) < PROMINENT_PEAK_HEIGHT_THRESHOLD) {
                        peaksToRemove.add(peak1);
                        peaksToRemove.add(peak2);
                    }
                }
            }
        }

        return numPeaks - peaksToRemove.size();
    }

    /* Compute number of weak peaks. Not necessarily (numPeaks - strongPeaks) */
    // TODO: Copy python pseudocode over to comments
    private static final double WEAK_PEAK_HEIGHT_THRESHOLD = 0.2;
    public int computeNumWeakPeaks(double[] signal, List<Integer> peakIndices) {
        Set<Integer> weakPeakIndices = new HashSet<Integer>();
        int numPeaks = peakIndices.size();
        int peak1 = 0;
        int peak2 = 0;

        /* Populate set with weak peaks */
        for (int i = 0; i < numPeaks; i++) {
            for (int j = 0; j < numPeaks; j++) {
                peak1 = peakIndices.get(i);
                peak2 = peakIndices.get(j);

                /* If peaks are neighbors but not referring to the same peak */
                if (isNeighbor(peak1, peak2, NEIGHBOR_THRESHOLD) && i != j) {
                    /* If too close, consider */
                    if (Math.abs(peak1 - peak2) < PEAK_LAG_THRESHOLD) {
                        /* If also not above height threshold, add to set */
                        if (Math.abs(signal[peak1] - signal[peak2]) < WEAK_PEAK_HEIGHT_THRESHOLD) {
                            weakPeakIndices.add(peak1);
                            weakPeakIndices.add(peak2);
                        }
                    }
                }
            }
        }

        return weakPeakIndices.size();
    }

    /* Helper function for weak/prominent peak computations */
    private boolean isNeighbor(int peak1, int peak2, int neighborThreshold) {
        if (Math.abs(peak1 - peak2) <= neighborThreshold) {
            return true;
        } else {
            return false;
        }
    }

    /* Find the maximum value given a set of array indices */
    public double findMaxPeakValue(double[] signal, List<Integer> peakIndices) {
        double maxValue = signal[peakIndices.get(0)];
        double curValue = 0;

        for (int peakIdx : peakIndices) {
            curValue = signal[peakIdx];
            if (curValue > maxValue) {
                maxValue = curValue;
            }
        }

        return maxValue;
    }
}
