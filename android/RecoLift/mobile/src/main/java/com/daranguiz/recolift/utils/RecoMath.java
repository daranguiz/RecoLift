package com.daranguiz.recolift.utils;

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
}
