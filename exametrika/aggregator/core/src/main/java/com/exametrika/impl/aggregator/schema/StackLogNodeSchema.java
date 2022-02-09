/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.schema;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.config.model.LogSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackErrorLogSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackLogSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.BackgroundRootNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.StackLogNodeSchemaConfiguration;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.INameNodeSchema;
import com.exametrika.api.aggregator.schema.IStackLogNodeSchema;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.spi.aggregator.IAggregationLogFilter;
import com.exametrika.spi.aggregator.IAggregationLogTransformer;
import com.exametrika.spi.aggregator.IErrorAggregationStrategy;
import com.exametrika.spi.aggregator.config.model.AggregationLogTransformerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ErrorAggregationStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link StackLogNodeSchema} represents a schema of stack log node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StackLogNodeSchema extends AggregationNodeSchema implements IStackLogNodeSchema {
    private IAggregationNodeSchema backgroundRoot;
    private final boolean allowHierarchyAggregation;
    private final boolean allowTransactionFailureAggregation;
    private IAggregationLogFilter logFilter;
    private List<IAggregationLogTransformer> logTransformers;
    private INameNodeSchema transactionFailureNode;
    private final NameFilter stackTraceFilter;
    private final List<IErrorAggregationStrategy> errorAggregationStrategies;
    private final NameFilter transactionFailureFilter;
    private final boolean transactionFailureErrorLog;

    public StackLogNodeSchema(StackLogNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                              IDocumentSchema fullTextSchema) {
        super(configuration, index, fields, fullTextSchema);

        boolean allowHierarchyAggregation = false;
        boolean allowTransactionFailureAggregation = false;

        StackLogSchemaConfiguration componentType = (StackLogSchemaConfiguration) configuration.getComponentType();
        if (componentType.isAllowHierarchyAggregation())
            allowHierarchyAggregation = true;

        if (componentType instanceof StackErrorLogSchemaConfiguration) {
            StackErrorLogSchemaConfiguration errorLog = (StackErrorLogSchemaConfiguration) componentType;
            if (errorLog.isAllowTransactionFailureAggregation())
                allowTransactionFailureAggregation = true;

            stackTraceFilter = errorLog.getStackTraceFilter();
            errorAggregationStrategies = new ArrayList<IErrorAggregationStrategy>();
            for (ErrorAggregationStrategySchemaConfiguration strategy : errorLog.getErrorAggregationStrategies())
                errorAggregationStrategies.add(strategy.createStrategy());
            transactionFailureErrorLog = errorLog.isTransactionFailureErrorLog();
            this.transactionFailureFilter = errorLog.getTransactionFailureFilter();
        } else {
            stackTraceFilter = null;
            errorAggregationStrategies = null;
            transactionFailureFilter = null;
            transactionFailureErrorLog = false;
        }

        this.allowHierarchyAggregation = allowHierarchyAggregation;
        this.allowTransactionFailureAggregation = allowTransactionFailureAggregation;
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        IDatabaseContext context = ((NodeSpaceSchema) getParent()).getContext();
        ICycleSchema cycleSchema = getParent();

        StackLogSchemaConfiguration componentType = (StackLogSchemaConfiguration) getConfiguration().getComponentType();

        if (getConfiguration().getComponentType().isLog()) {
            LogSchemaConfiguration logConfiguration = ((LogSchemaConfiguration) getConfiguration().getComponentType().getMetricTypes().get(0));
            if (logConfiguration.getFilter() != null)
                logFilter = logConfiguration.getFilter().createFilter(context);
            else
                logFilter = null;

            logTransformers = new ArrayList<IAggregationLogTransformer>();
            for (AggregationLogTransformerSchemaConfiguration transformer : logConfiguration.getTransformers())
                logTransformers.add(transformer.createTransformer(context));
        } else {
            logFilter = null;
            logTransformers = null;
        }

        transactionFailureNode = null;
        if (componentType instanceof StackErrorLogSchemaConfiguration) {
            StackErrorLogSchemaConfiguration errorLog = (StackErrorLogSchemaConfiguration) componentType;
            if (errorLog.getTransactionFailureComponentType() != null)
                transactionFailureNode = (INameNodeSchema) cycleSchema.findAggregationNode(errorLog.getTransactionFailureComponentType());
        }

        for (INodeSchema schema : cycleSchema.getNodes()) {
            if (schema.getConfiguration() instanceof BackgroundRootNodeSchemaConfiguration) {
                backgroundRoot = (IAggregationNodeSchema) schema;
                break;
            }
        }
    }

    @Override
    public IAggregationNodeSchema getBackgroundRoot() {
        return backgroundRoot;
    }

    @Override
    public boolean isAllowHierarchyAggregation() {
        return allowHierarchyAggregation;
    }

    @Override
    public boolean isAllowTransactionFailureAggregation() {
        return allowTransactionFailureAggregation;
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
    public INameNodeSchema getTransactionFailureNode() {
        return transactionFailureNode;
    }

    @Override
    public NameFilter getStackTraceFilter() {
        return stackTraceFilter;
    }

    @Override
    public List<IErrorAggregationStrategy> getErrorAggregationStrategies() {
        return errorAggregationStrategies;
    }

    @Override
    public NameFilter getTransactionFailureFilter() {
        return transactionFailureFilter;
    }

    @Override
    public boolean isTransactionFailureErrorLog() {
        return transactionFailureErrorLog;
    }
}
