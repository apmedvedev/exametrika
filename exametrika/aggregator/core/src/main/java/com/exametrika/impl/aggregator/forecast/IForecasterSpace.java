/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast;


/**
 * The {@link IForecasterSpace} represents a forecaster space.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IForecasterSpace {
    /**
     * Creates forecaster.
     *
     * @param parameters parameters
     * @return forecaster
     */
    IForecaster createForecaster(Forecaster.Parameters parameters);

    /**
     * Opens forecaster.
     *
     * @param id         identifier
     * @param parameters parameters
     * @return forecaster
     */
    IForecaster openForecaster(int id, Forecaster.Parameters parameters);
}
