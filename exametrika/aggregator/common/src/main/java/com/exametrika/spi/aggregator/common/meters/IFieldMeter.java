/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;

import com.exametrika.common.json.JsonObject;


/**
 * The {@link IFieldMeter} represents a field based meter.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IFieldMeter extends IMeter {
    /**
     * Returns true if meters has instance fields.
     *
     * @return true if meters has instance fields
     */
    boolean hasInstanceFields();

    /**
     * Sets context for instance-based metering. Context is bounded to current thread, shared between all meters
     * and affects all upcoming measurements.
     *
     * @param context context for instance-based metering. Can be null if not set
     */
    void setInstanceContext(JsonObject context);
}
