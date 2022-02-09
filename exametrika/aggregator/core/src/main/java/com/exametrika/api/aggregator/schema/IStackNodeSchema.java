/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.schema;

import java.util.List;

import com.exametrika.spi.aggregator.IComponentDeletionStrategy;
import com.exametrika.spi.aggregator.IComponentDiscoveryStrategy;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;


/**
 * The {@link IStackNodeSchema} represents a schema for stack node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IStackNodeSchema extends IAggregationNodeSchema {
    /**
     * Returns list of scope aggregation strategies.
     *
     * @return list of scope aggregation strategies
     */
    List<IScopeAggregationStrategy> getScopeAggregationStrategies();

    /**
     * Is hierarchy aggregation allowed?
     *
     * @return true if hierarchy aggregation is allowed
     */
    boolean isAllowHierarchyAggregation();

    /**
     * Is stack name aggregation allowed?
     *
     * @return true if stack name aggregation is allowed
     */
    boolean isAllowStackNameAggregation();

    /**
     * Returns schema of stack name node.
     *
     * @return schema of stack name node or null if stack name node schema is not set
     */
    INameNodeSchema getStackNameNode();

    /**
     * Is transaction failure dependencies aggregation allowed?
     *
     * @return true if transaction failure dependencies aggregation is allowed
     */
    boolean isAllowTransactionFailureDependenciesAggregation();

    /**
     * Is anomalies correlation allowed?
     *
     * @return true if anomalies correlation is allowed
     */
    boolean isAllowAnomaliesCorrelation();

    /**
     * Returns schema of transaction failure depedencies node.
     *
     * @return schema of transaction failure depedencies node or null if transaction failure depedencies node schema is not set
     */
    IStackLogNodeSchema getTransactionFailureDependenciesNode();

    /**
     * Returns schema of anomalies node.
     *
     * @return schema of anomalies node or null if anomalies node schema is not set
     */
    IStackLogNodeSchema getAnomaliesNode();

    /**
     * Returns component discovery strategies.
     *
     * @return component discovery strategies
     */
    List<IComponentDiscoveryStrategy> getComponentDiscoveryStrategies();

    /**
     * Returns component deletion strategy.
     *
     * @return component deletion strategy or null if strategy is not set
     */
    IComponentDeletionStrategy getComponentDeletionStrategy();

    /**
     * Returns index of stackIds metric.
     *
     * @return index of stackIds metric
     */
    int getStackIdsMetricIndex();
}
