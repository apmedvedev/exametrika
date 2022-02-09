/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.schema;

import java.util.List;

import com.exametrika.spi.aggregator.IAggregationFilter;
import com.exametrika.spi.aggregator.IAggregationLogFilter;
import com.exametrika.spi.aggregator.IAggregationLogTransformer;
import com.exametrika.spi.aggregator.IComponentDeletionStrategy;
import com.exametrika.spi.aggregator.IComponentDiscoveryStrategy;
import com.exametrika.spi.aggregator.IMetricAggregationStrategy;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;


/**
 * The {@link INameNodeSchema} represents a schema for name node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface INameNodeSchema extends IAggregationNodeSchema {
    /**
     * Does component type have sumByGroup metricTypes (gauges).
     *
     * @return true if component type have sumByGroup metricTypes (gauges)
     */
    boolean hasSumByGroupMetrics();

    /**
     * Returns list of scope aggregation strategies.
     *
     * @return list of scope aggregation strategies
     */
    List<IScopeAggregationStrategy> getScopeAggregationStrategies();

    /**
     * Returns list of metric aggregation strategies.
     *
     * @return list of metric aggregation strategies
     */
    List<IMetricAggregationStrategy> getMetricAggregationStrategies();

    /**
     * Returns aggregation filter.
     *
     * @return aggregation filter or null if filter is not set
     */
    IAggregationFilter getAggregationFilter();

    /**
     * Is hierarchy aggregation allowed?
     *
     * @return true if hierarchy aggregation is allowed
     */
    boolean isAllowHierarchyAggregation();

    /**
     * Is transfer of derived measurements to next period type allowed?
     *
     * @return true if transfer of derived measurements to next period type is allowed
     */
    boolean isAllowTransferDerived();

    /**
     * Returns aggregation log filter.
     *
     * @return aggregation log filter or null if filter is not set
     */
    IAggregationLogFilter getLogFilter();

    /**
     * Returns aggregation log transformers.
     *
     * @return aggregation log transformers
     */
    List<IAggregationLogTransformer> getLogTransformers();

    /**
     * Returns component discovery strategies.
     *
     * @return component discovery strategies
     */
    List<IComponentDiscoveryStrategy> getComponentDiscoveryStrategies();

    /**
     * Returns component deletion strategy.
     *
     * @return component deletion strategy or null if component deletion strategy is not used
     */
    IComponentDeletionStrategy getComponentDeletionStrategy();
}
