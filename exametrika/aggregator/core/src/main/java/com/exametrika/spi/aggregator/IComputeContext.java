/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.Measurement;


/**
 * The {@link IComputeContext} represents a context for computing Json representation of field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IComputeContext {
    /**
     * Returns current node type.
     *
     * @return current node type
     */
    String getNodeType();

    /**
     * Returns current context object used in navigation accessors.
     *
     * @return context object used in navigation accessors
     */
    Object getObject();

    /**
     * Is inherent part of value computed? For stack values only.
     *
     * @return true if inherent part of value is computed
     */
    boolean isInherent();

    /**
     * Is total part of value computed? For stack values only.
     *
     * @return true if total part of value is computed
     */
    boolean isTotal();

    /**
     * Returns end time of aggregation.
     *
     * @return end time of aggregation
     */
    long getTime();

    /**
     * Sets time.
     *
     * @param time time
     */
    void setTime(long time);

    /**
     * Returns period of computation.
     *
     * @return period of computation
     */
    long getPeriod();

    /**
     * Sets period.
     *
     * @param period period
     */
    void setPeriod(long period);

    /**
     * Returns name manager.
     *
     * @return name manager
     */
    IPeriodNameManager getNameManager();

    /**
     * Adds derived measurement.
     *
     * @param measurement measurement
     */
    void addMeasurement(Measurement measurement);
}
