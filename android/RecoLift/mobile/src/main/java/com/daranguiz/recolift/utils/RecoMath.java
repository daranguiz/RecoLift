package com.daranguiz.recolift.utils;

import Jama.Matrix;

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
            mean[i] = getDoubleArrayMean(buffer[i]);
        }
        double zeroMeanMat[][] = inputMat.getArray();
        for (int i = 0; i < NUM_DOFS; i++) {
            for (int j = 0; j < WINDOW_SIZE; j++) {
                zeroMeanMat[i][j] = zeroMeanMat[i][j] - mean[i];
            }
        }

        /* Compute the scatter matrix, should be 3x3 */
        Matrix scatterMat = inputMat.times(inputMat.transpose());

        return scatterMat;
    }

    /* Compute mean of an array */
    public double getDoubleArrayMean(double[] arr) {
        double mean = 0;

        for (int i = 0; i < arr.length; i++) {
            mean += arr[i];
        }
        mean /= arr.length;

        return mean;
    }
}
