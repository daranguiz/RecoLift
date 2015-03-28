package com.daranguiz.recolift.utils;

import Jama.EigenvalueDecomposition;
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
}
