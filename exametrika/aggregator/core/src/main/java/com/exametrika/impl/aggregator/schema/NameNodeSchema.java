/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.schema;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.config.model.GaugeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.LogSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.MetricTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.NameNodeSchemaConfiguration;
import com.exametrika.api.aggregator.schema.INameNodeSchema;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.spi.aggregator.IAggregationFilter;
import com.exametrika.spi.aggregator.IAggregationLogFilter;
import com.exametrika.spi.aggregator.IAggregationLogTransformer;
import com.exametrika.spi.aggregator.IComponentDeletionStrategy;
import com.exametrika.spi.aggregator.IComponentDiscoveryStrategy;
import com.exametrika.spi.aggregator.IMetricAggregationStrategy;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;
import com.exametrika.spi.aggregator.config.model.AggregationLogTransformerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MetricAggregationStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link NameNodeSchema} represents a schema of name node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class NameNodeSchema extends AggregationNodeSchema implements INameNodeSchema {
    private boolean allowTransferDerived;
    private boolean allowHierarchyAggregation;
    private final boolean hasSumByGroupMetrics;
    private List<IScopeAggregationStrategy> scopeAggregationStrategies;
    private List<IMetricAggregationStrategy> metricAggregationStrategies;
    private IAggregationFilter aggregationFilter;
    private IAggregationLogFilter logFilter;
    private List<IAggregationLogTransformer> logTransformers;
    private List<IComponentDiscoveryStrategy> componentDiscoveryStrategies;
    private IComponentDeletionStrategy componentDeletionStrategy;

    public NameNodeSchema(NameNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                          IDocumentSchema fullTextSchema) {
        super(configuration, index, fields, fullTextSchema);

        boolean allowTransferDerived = false;
        boolean allowHierarchyAggregation = false;

        NameSchemaConfiguration componentType = (NameSchemaConfiguration) configuration.getComponentType();
        if (componentType.isAllowTransferDerived())
            allowTransferDerived = true;
        if (componentType.isAllowHierarchyAggregation())
            allowHierarchyAggregation = true;

        this.allowTransferDerived = allowTransferDerived;
        this.allowHierarchyAggregation = allowHierarchyAggregation;

        boolean hasSumByGroupMetrics = false;
        for (MetricTypeSchemaConfiguration metricType : configuration.getComponentType().getMetricTypes()) {
            if (metricType instanceof GaugeSchemaConfiguration && ((GaugeSchemaConfiguration) metricType).isSumByGroup()) {
                hasSumByGroupMetrics = true;
                break;
            }
        }

        this.hasSumByGroupMetrics = hasSumByGroupMetrics;
    }

    @Override
    public boolean hasSumByGroupMetrics() {
        return hasSumByGroupMetrics;
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        IDatabaseContext context = ((NodeSpaceSchema) getParent()).getContext();
        NameSchemaConfiguration componentType = (NameSchemaConfiguration) getConfiguration().getComponentType();

        scopeAggregationStrategies = new ArrayList<IScopeAggregationStrategy>();
        for (ScopeAggregationStrategySchemaConfiguration scopeAggregationStrategy : componentType.getScopeAggregationStrategies())
            scopeAggregationStrategies.add(scopeAggregationStrategy.createStrategy(context));

        metricAggregationStrategies = new ArrayList<IMetricAggregationStrategy>();
        for (MetricAggregationStrategySchemaConfiguration metricAggregationStrategy : componentType.getMetricAggregationStrategies())
            metricAggregationStrategies.add(metricAggregationStrategy.createStrategy(context));

        if (componentType.getAggregationFilter() != null)
            aggregationFilter = componentType.getAggregationFilter().createFilter(context);
        else
            aggregationFilter = null;

        if (getConfiguration().getComponentType().isLog()) {
            LogSchemaConfiguration logConfiguration = ((LogSchemaConfiguration) getConfiguration().getComponentType().getMetricTypes().get(0));
            if (logConfiguration.getFilter() != null)
                logFilter = logConfiguration.getFilter().createFilter(context);

            logTransformers = new ArrayList<IAggregationLogTransformer>();
            for (AggregationLogTransformerSchemaConfiguration transformer : logConfiguration.getTransformers())
                logTransformers.add(transformer.createTransformer(context));
        } else {
            logFilter = null;
            logTransformers = null;
        }

        componentDiscoveryStrategies = new ArrayList<IComponentDiscoveryStrategy>();
        for (ComponentDiscoveryStrategySchemaConfiguration strategy : componentType.getComponentDiscoveryStrategies())
            componentDiscoveryStrategies.add(strategy.createStrategy(context));
        if (componentType.getComponentDeletionStrategy() != null)
            componentDeletionStrategy = componentType.getComponentDeletionStrategy().createStrategy(context);
        else
            componentDeletionStrategy = null;

        if (allowTransferDerived) {
            INameNodeSchema prevNodeSchema = (INameNodeSchema) getPreviousPeriodNode();
            while (prevNodeSchema != null) {
                if (prevNodeSchema.isAllowTransferDerived()) {
                    allowTransferDerived = false;
                    allowHierarchyAggregation = false;
                    break;
                }
                prevNodeSchema = (INameNodeSchema) prevNodeSchema.getPreviousPeriodNode();
            }
        }
    }

    @Override
    public List<IScopeAggregationStrategy> getScopeAggregationStrategies() {
        return scopeAggregationStrategies;
    }

    @Override
    public List<IMetricAggregationStrategy> getMetricAggregationStrategies() {
        return metricAggregationStrategies;
    }

    @Override
    public IAggregationFilter getAggregationFilter() {
        return aggregationFilter;
    }

    @Override
    public boolean isAllowHierarchyAggregation() {
        return allowHierarchyAggregation;
    }

    @Override
    public boolean isAllowTransferDerived() {
        return allowTransferDerived;
    }

    @Override
    public IAggregationLogFilter getLogFilter() {
        return logFilter;
    }

    @Override
    public List<IAggregationLogTransformer> getLogTransformers() {
        return logTransformers;
    }

    @Override
    public List<IComponentDiscoveryStrategy> getComponentDiscoveryStrategies() {
        return componentDiscoveryStrategies;
    }

    @Override
    public IComponentDeletionStrategy getComponentDeletionStrategy() {
        return componentDeletionStrategy;
    }
}
