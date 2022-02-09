/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.schema;

import java.util.List;

import com.exametrika.common.utils.NameFilter;
import com.exametrika.spi.aggregator.IAggregationLogFilter;
import com.exametrika.spi.aggregator.IAggregationLogTransformer;
import com.exametrika.spi.aggregator.IErrorAggregationStrategy;


/**
 * The {@link IStackLogNodeSchema} represents a schema for stack log node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IStackLogNodeSchema extends IAggregationNodeSchema {
    /**
     * Is hierarchy aggregation allowed?
     *
     * @return true if hierarchy aggregation is allowed
     */
    boolean isAllowHierarchyAggregation();

    /**
     * Is transaction failure aggregation allowed?
     *
     * @return true if transaction failure aggregation is allowed
     */
    boolean isAllowTransactionFailureAggregation();

    /**
     * Returns schema of background root node.
     *
     * @return schema of background root node or null if schema is not found
     */
    IAggregationNodeSchema getBackgroundRoot();

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
     * Returns schema of transaction failure node.
     *
     * @return schema of transaction failure node or null if transaction failure node schema is not set
     */
    INameNodeSchema getTransactionFailureNode();

    /**
     * Returns stack trace filter.
     *
     * @return stack trace filter or null if stack trace filter is not set
     */
    NameFilter getStackTraceFilter();

    /**
     * Returns list of error aggregation strategies.
     *
     * @return list of error aggregation strategies
     */
    List<IErrorAggregationStrategy> getErrorAggregationStrategies();

    /**
     * Returns transaction failure filter.
     *
     * @return transaction failure filter or null if transaction failure filter is not set
     */
    NameFilter getTransactionFailureFilter();

    /**
     * Is log a main transaction failure error log?
     *
     * @return true if log is main transaction failure error log
     */
    boolean isTransactionFailureErrorLog();
}
