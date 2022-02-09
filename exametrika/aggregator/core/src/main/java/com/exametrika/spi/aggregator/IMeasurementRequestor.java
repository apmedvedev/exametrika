/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.common.utils.ICompletionHandler;


/**
 * The {@link IMeasurementRequestor} represents an requestor of agent measurements.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMeasurementRequestor {
    String NAME = "measurementRequestor";

    /**
     * Initiates measurement requests from all currently connected agent and notifies specified completion handler when
     * measurements or failures from all agents are received.
     *
     * @param completionHandler completion handler called in any context when measurements or failures from all agents are received
     */
    void requestMeasurements(ICompletionHandler completionHandler);
}
