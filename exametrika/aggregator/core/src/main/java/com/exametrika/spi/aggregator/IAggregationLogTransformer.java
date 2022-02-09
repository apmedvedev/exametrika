/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import java.util.List;

import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.nodes.IAggregationNode;


/**
 * The {@link IAggregationLogTransformer} represents an aggregation log transformer.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAggregationLogTransformer {
    /**
     * Transforms log records to derived measurements.
     *
     * @param node log aggregation node
     * @return list of derived log measurements
     */
    List<Measurement> transform(IAggregationNode node);
}
