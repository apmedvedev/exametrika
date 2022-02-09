/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.schema;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.config.schema.AggregationNodeSchemaConfiguration;
import com.exametrika.api.aggregator.schema.IAggregationFieldSchema;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.spi.aggregator.IAggregationAnalyzer;
import com.exametrika.spi.aggregator.IComponentBindingStrategy;
import com.exametrika.spi.aggregator.IMeasurementFilter;
import com.exametrika.spi.aggregator.config.model.AggregationAnalyzerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentBindingStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MeasurementFilterSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link AggregationNodeSchema} represents a schema of aggregation node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AggregationNodeSchema extends PeriodNodeSchema implements IAggregationNodeSchema {
    private final IAggregationFieldSchema aggregationField;
    private IMeasurementFilter filter;
    private List<IComponentBindingStrategy> componentBindingStrategies;
    private List<IAggregationAnalyzer> analyzers;

    public AggregationNodeSchema(AggregationNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                                 IDocumentSchema fullTextSchema) {
        super(configuration, index, fields, fullTextSchema);

        aggregationField = (IAggregationFieldSchema) findField(configuration.getAggregationField().getName());
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        IDatabaseContext context = ((NodeSpaceSchema) getParent()).getContext();

        MeasurementFilterSchemaConfiguration filterConfiguration = getConfiguration().getComponentType().getFilter();
        if (filterConfiguration != null)
            filter = filterConfiguration.createFilter(context);
        else
            filter = null;

        componentBindingStrategies = new ArrayList<IComponentBindingStrategy>();
        for (ComponentBindingStrategySchemaConfiguration strategy : getConfiguration().getComponentType().getComponentBindingStrategies())
            componentBindingStrategies.add(strategy.createStrategy(context));

        analyzers = new ArrayList<IAggregationAnalyzer>();
        for (AggregationAnalyzerSchemaConfiguration strategy : getConfiguration().getComponentType().getAnalyzers())
            analyzers.add(strategy.createAnalyzer(context));
    }

    @Override
    public AggregationNodeSchemaConfiguration getConfiguration() {
        return (AggregationNodeSchemaConfiguration) super.getConfiguration();
    }

    @Override
    public IAggregationNodeSchema getPreviousPeriodNode() {
        return (IAggregationNodeSchema) super.getPreviousPeriodNode();
    }

    @Override
    public IAggregationNodeSchema getNextPeriodNode() {
        return (IAggregationNodeSchema) super.getNextPeriodNode();
    }

    @Override
    public IMeasurementFilter getFilter() {
        return filter;
    }

    @Override
    public boolean isMetadataRequired() {
        return false;
    }

    @Override
    public final IAggregationFieldSchema getAggregationField() {
        return aggregationField;
    }

    @Override
    public List<IComponentBindingStrategy> getComponentBindingStrategies() {
        return componentBindingStrategies;
    }

    @Override
    public List<IAggregationAnalyzer> getAnalyzers() {
        return analyzers;
    }
}
