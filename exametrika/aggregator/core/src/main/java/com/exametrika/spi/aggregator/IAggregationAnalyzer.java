/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.common.json.JsonObjectBuilder;


/**
 * The {@link IAggregationAnalyzer} represents an analyzer of aggregation nodes.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IAggregationAnalyzer {
    /**
     * Analyzes specified node and possibly nodes reachable from it.
     *
     * @param node   aggregation node
     * @param result json object to place analysis results to
     */
    void analyze(IAggregationNode node, JsonObjectBuilder result);
}
