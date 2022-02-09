/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.schema;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StackIdsValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.BackgroundRootSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PrimaryEntryPointSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.StackNodeSchemaConfiguration;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.INameNodeSchema;
import com.exametrika.api.aggregator.schema.IStackLogNodeSchema;
import com.exametrika.api.aggregator.schema.IStackNodeSchema;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.spi.aggregator.IComponentDeletionStrategy;
import com.exametrika.spi.aggregator.IComponentDiscoveryStrategy;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;
import com.exametrika.spi.aggregator.config.model.ComponentDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link StackNodeSchema} represents a schema of aggregation node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StackNodeSchema extends AggregationNodeSchema implements IStackNodeSchema {
    private List<IScopeAggregationStrategy> scopeAggregationStrategies;
    private INameNodeSchema stackNameNode;
    private final boolean allowHierarchyAggregation;
    private final boolean allowStackNameAggregation;
    private final boolean allowTransactionFailureDependeciesAggregation;
    private final boolean allowAnomaliesCorrelation;
    private IStackLogNodeSchema transactionFailureDependenciesNode;
    private IStackLogNodeSchema anomaliesNode;
    private List<IComponentDiscoveryStrategy> componentDiscoveryStrategies;
    private IComponentDeletionStrategy componentDeletionStrategy;
    private final int stackIdsMetricIndex;

    public StackNodeSchema(StackNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                           IDocumentSchema fullTextSchema) {
        super(configuration, index, fields, fullTextSchema);

        boolean allowHierarchyAggregation = false;
        boolean allowStackNameAggregation = false;
        boolean allowTransactionFailureDependeciesAggregation = false;
        boolean allowAnomaliesCorrelation = false;
        if (configuration.getComponentType() instanceof BackgroundRootSchemaConfiguration) {
            BackgroundRootSchemaConfiguration componentType = (BackgroundRootSchemaConfiguration) configuration.getComponentType();
            if (componentType.isAllowHierarchyAggregation())
                allowHierarchyAggregation = true;
            if (componentType.isAllowStackNameAggregation())
                allowStackNameAggregation = true;
            if (componentType.isAllowAnomaliesCorrelation())
                allowAnomaliesCorrelation = true;
        } else if (configuration.getComponentType() instanceof PrimaryEntryPointSchemaConfiguration) {
            PrimaryEntryPointSchemaConfiguration componentType = (PrimaryEntryPointSchemaConfiguration) configuration.getComponentType();
            if (componentType.isAllowHierarchyAggregation())
                allowHierarchyAggregation = true;
            if (componentType.isAllowStackNameAggregation())
                allowStackNameAggregation = true;
            if (componentType.isAllowTransactionFailureDependenciesAggregation())
                allowTransactionFailureDependeciesAggregation = true;
            if (componentType.isAllowAnomaliesCorrelation())
                allowAnomaliesCorrelation = true;
        }

        this.allowHierarchyAggregation = allowHierarchyAggregation;
        this.allowStackNameAggregation = allowStackNameAggregation;
        this.allowTransactionFailureDependeciesAggregation = allowTransactionFailureDependeciesAggregation;
        this.allowAnomaliesCorrelation = allowAnomaliesCorrelation;

        int stackIdsMetricIndex = -1;
        int i = 0;
        for (MetricValueSchemaConfiguration metric : configuration.getComponentType().getMetrics().getMetrics()) {
            if (metric instanceof StackIdsValueSchemaConfiguration) {
                stackIdsMetricIndex = i;
                break;
            }
            i++;
        }
        this.stackIdsMetricIndex = stackIdsMetricIndex;
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        IDatabaseContext context = ((NodeSpaceSchema) getParent()).getContext();

        ICycleSchema cycleSchema = getParent();

        StackSchemaConfiguration componentType = (StackSchemaConfiguration) getConfiguration().getComponentType();

        scopeAggregationStrategies = null;
        anomaliesNode = null;
        transactionFailureDependenciesNode = null;
        stackNameNode = null;
        componentDiscoveryStrategies = new ArrayList<IComponentDiscoveryStrategy>();
        if (componentType instanceof BackgroundRootSchemaConfiguration) {
            BackgroundRootSchemaConfiguration rootComponentType = (BackgroundRootSchemaConfiguration) componentType;
            scopeAggregationStrategies = new ArrayList<IScopeAggregationStrategy>();
            for (ScopeAggregationStrategySchemaConfiguration scopeAggregationStrategy : rootComponentType.getScopeAggregationStrategies())
                scopeAggregationStrategies.add(scopeAggregationStrategy.createStrategy(context));
            if (rootComponentType.getAnomaliesComponentType() != null)
                anomaliesNode = (IStackLogNodeSchema) cycleSchema.findAggregationNode(
                        rootComponentType.getAnomaliesComponentType());
        } else if (componentType instanceof PrimaryEntryPointSchemaConfiguration) {
            PrimaryEntryPointSchemaConfiguration primaryComponentType = (PrimaryEntryPointSchemaConfiguration) componentType;
            scopeAggregationStrategies = new ArrayList<IScopeAggregationStrategy>();
            for (ScopeAggregationStrategySchemaConfiguration scopeAggregationStrategy : primaryComponentType.getScopeAggregationStrategies())
                scopeAggregationStrategies.add(scopeAggregationStrategy.createStrategy(context));
            if (primaryComponentType.getTransactionFailureDependenciesComponentType() != null)
                transactionFailureDependenciesNode = (IStackLogNodeSchema) cycleSchema.findAggregationNode(
                        primaryComponentType.getTransactionFailureDependenciesComponentType());
            if (primaryComponentType.getAnomaliesComponentType() != null)
                anomaliesNode = (IStackLogNodeSchema) cycleSchema.findAggregationNode(
                        primaryComponentType.getAnomaliesComponentType());

            for (ComponentDiscoveryStrategySchemaConfiguration strategy : primaryComponentType.getComponentDiscoveryStrategies())
                componentDiscoveryStrategies.add(strategy.createStrategy(context));

            if (primaryComponentType.getComponentDeletionStrategy() != null)
                componentDeletionStrategy = primaryComponentType.getComponentDeletionStrategy().createStrategy(context);
        }

        if (componentType.getStackNameComponentType() != null) {
            stackNameNode = (INameNodeSchema) cycleSchema.findAggregationNode(componentType.getStackNameComponentType());
            Assert.notNull(stackNameNode);
        }
    }

    @Override
    public INameNodeSchema getStackNameNode() {
        return stackNameNode;
    }

    @Override
    public boolean isMetadataRequired() {
        return true;
    }

    @Override
    public List<IScopeAggregationStrategy> getScopeAggregationStrategies() {
        return scopeAggregationStrategies;
    }

    @Override
    public boolean isAllowHierarchyAggregation() {
        return allowHierarchyAggregation;
    }

    @Override
    public boolean isAllowStackNameAggregation() {
        return allowStackNameAggregation;
    }

    @Override
    public boolean isAllowTransactionFailureDependenciesAggregation() {
        return allowTransactionFailureDependeciesAggregation;
    }

    @Override
    public boolean isAllowAnomaliesCorrelation() {
        return allowAnomaliesCorrelation;
    }

    @Override
    public IStackLogNodeSchema getTransactionFailureDependenciesNode() {
        return transactionFailureDependenciesNode;
    }

    @Override
    public IStackLogNodeSchema getAnomaliesNode() {
        return anomaliesNode;
    }

    @Override
    public List<IComponentDiscoveryStrategy> getComponentDiscoveryStrategies() {
        return componentDiscoveryStrategies;
    }

    @Override
    public IComponentDeletionStrategy getComponentDeletionStrategy() {
        return componentDeletionStrategy;
    }

    @Override
    public int getStackIdsMetricIndex() {
        return stackIdsMetricIndex;
    }
}
