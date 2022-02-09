/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;

import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;


/**
 * The {@link IFieldFactory} represents a field factory.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IFieldFactory {
    /**
     * Creates a field collector.
     *
     * @return field collector
     */
    IFieldCollector createCollector();

    /**
     * Creates field value serializer.
     *
     * @return field value serializer or null if not used
     */
    IFieldValueSerializer createValueSerializer();
}
