/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast.errors;

import java.util.ArrayDeque;
import java.util.Deque;

import com.exametrika.common.utils.Assert;


/**
 * The {@link PredictionErrorEstimator} is a helper object to estimate prediction errors.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PredictionErrorEstimator {
    private final int stepCount;
    private final Deque<Float>[] predictionQueues;
    private final MeanAbsoluteScaledErrorMetric[] maseErrors;
    private final MeanAbsoluteErrorMetric[] maeErrors;
    private final MeanAbsolutePercentageErrorMetric[] mapeErrors;
    private final AlternativeMeanAbsolutePercentageErrorMetric[] altMapeErrors;
    private final RootMeanSquareErrorMetric[] rmseErrors;
    private int iterationCount;

    public PredictionErrorEstimator(int stepCount) {
        this.stepCount = stepCount;
        predictionQueues = new Deque[stepCount];
        maseErrors = new MeanAbsoluteScaledErrorMetric[stepCount];
        maeErrors = new MeanAbsoluteErrorMetric[stepCount];
        mapeErrors = new MeanAbsolutePercentageErrorMetric[stepCount];
        altMapeErrors = new AlternativeMeanAbsolutePercentageErrorMetric[stepCount];
        rmseErrors = new RootMeanSquareErrorMetric[stepCount];
        for (int i = 0; i < stepCount; i++) {
            predictionQueues[i] = new ArrayDeque<Float>();
            maseErrors[i] = new MeanAbsoluteScaledErrorMetric();
            maeErrors[i] = new MeanAbsoluteErrorMetric();
            mapeErrors[i] = new MeanAbsolutePercentageErrorMetric();
            altMapeErrors[i] = new AlternativeMeanAbsolutePercentageErrorMetric();
            rmseErrors[i] = new RootMeanSquareErrorMetric();
        }
    }

    public double getMaseError(int index) {
        return maseErrors[index].getMetric();
    }

    public double getMaeError(int index) {
        return maeErrors[index].getMetric();
    }

    public double getMapeError(int index) {
        return mapeErrors[index].getMetric();
    }

    public double getAltMapeError(int index) {
        return altMapeErrors[index].getMetric();
    }

    public double getRmseError(int index) {
        return rmseErrors[index].getMetric();
    }

    public float[] compute(float[] predictions) {
        Assert.isTrue(predictions.length == stepCount);

        float[] values = new float[stepCount];
        iterationCount++;

        for (int i = 0; i < stepCount; i++) {
            float value = predictions[i];
            predictionQueues[i].addLast(value);
            float first = Float.NaN;
            if (predictionQueues[i].size() > i + 1)
                first = predictionQueues[i].removeFirst();

            values[i] = first;

            if ((iterationCount % 100) == 0) {
                maseErrors[i].clear();
                maeErrors[i].clear();
                mapeErrors[i].clear();
                altMapeErrors[i].clear();
                rmseErrors[i].clear();
            }

            if (!Float.isNaN(first)) {
                maseErrors[i].compute(value, first);
                maeErrors[i].compute(value, first);
                mapeErrors[i].compute(value, first);
                altMapeErrors[i].compute(value, first);
                rmseErrors[i].compute(value, first);
            }
        }

        return values;
    }
}
