/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast;

import java.util.List;


/**
 * The {@link IForecaster} represents a forecaster.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IForecaster extends IAnomalyDetector {
    /**
     * Computes predictions.
     *
     * @param stepCount number of prediction steps
     * @return list of results for each prediction step
     */
    List<PredictionResult> computePredictions(int stepCount);
}
