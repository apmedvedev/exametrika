/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.schema;

import java.util.List;

import com.exametrika.api.aggregator.config.schema.AggregationNodeSchemaConfiguration;
import com.exametrika.spi.aggregator.IAggregationAnalyzer;
import com.exametrika.spi.aggregator.IComponentBindingStrategy;
import com.exametrika.spi.aggregator.IMeasurementFilter;


/**
 * The {@link IAggregationNodeSchema} represents a schema for aggregation node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAggregationNodeSchema extends IPeriodNodeSchema {
    @Override
    AggregationNodeSchemaConfiguration getConfiguration();

    @Override
    IAggregationNodeSchema getPreviousPeriodNode();

    @Override
    IAggregationNodeSchema getNextPeriodNode();

    /**
     * Is metadata required for initial creation of node?
     *
     * @return true if metadata is required for initial creation of node
     */
    boolean isMetadataRequired();

    /**
     * Returns filter of measurements.
     *
     * @return filter of measurements or null if filter is not set
     */
    IMeasurementFilter getFilter();

    /**
     * Returns component binding strategies.
     *
     * @return component binding strategies
     */
    List<IComponentBindingStrategy> getComponentBindingStrategies();

    /**
     * Returns aggregation analyzers.
     *
     * @return aggregation analyzers
     */
    List<IAggregationAnalyzer> getAnalyzers();

    /**
     * Returns aggregation field
     *
     * @return aggregation field
     */
    IAggregationFieldSchema getAggregationField();
}
