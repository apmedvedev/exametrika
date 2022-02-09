/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.config.model.AggregationLogTransformerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ErrorAggregationStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MeasurementFilterSchemaConfiguration;


/**
 * The {@link StackErrorLogSchemaConfiguration} is a stack error log component type schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StackErrorLogSchemaConfiguration extends StackLogSchemaConfiguration {
    private final boolean allowTypedErrorAggregation;
    private final boolean allowTransactionFailureAggregation;
    private final String errorComponentType;
    private final String transactionFailureComponentType;
    private final NameFilter stackTraceFilter;
    private final List<ErrorAggregationStrategySchemaConfiguration> errorAggregationStrategies;
    private final boolean transactionFailureErrorLog;
    private final NameFilter transactionFailureFilter;

    public StackErrorLogSchemaConfiguration(String name, LogSchemaConfiguration metric,
                                            MeasurementFilterSchemaConfiguration filter,
                                            boolean allowHierarchyAggregation, boolean allowTypedErrorAggregation,
                                            boolean allowTransactionFailureAggregation, String errorComponentType, String transactionFailureComponentType,
                                            NameFilter stackTraceFilter, List<? extends ErrorAggregationStrategySchemaConfiguration> errorAggregationStrategies,
                                            NameFilter transactionFailureFilter, boolean transactionFailureErrorLog) {
        super(name, Collections.singletonList(buildLogMetric(metric, allowTypedErrorAggregation, errorComponentType,
                stackTraceFilter, errorAggregationStrategies)), true, filter, null, null, null, allowHierarchyAggregation);

        Assert.notNull(!allowTypedErrorAggregation || errorComponentType != null);
        Assert.notNull(!allowTransactionFailureAggregation || transactionFailureComponentType != null);
        Assert.notNull(errorAggregationStrategies);

        this.allowTypedErrorAggregation = allowTypedErrorAggregation;
        this.allowTransactionFailureAggregation = allowTransactionFailureAggregation;
        this.errorComponentType = errorComponentType;
        this.transactionFailureComponentType = transactionFailureComponentType;
        this.stackTraceFilter = stackTraceFilter;
        this.errorAggregationStrategies = Immutables.wrap(errorAggregationStrategies);
        this.transactionFailureFilter = transactionFailureFilter;
        this.transactionFailureErrorLog = transactionFailureErrorLog;
    }

    @Override
    public Kind getKind() {
        return Kind.STACK_ERROR_LOG;
    }

    public boolean isAllowTypedErrorAggregation() {
        return allowTypedErrorAggregation;
    }

    public boolean isAllowTransactionFailureAggregation() {
        return allowTransactionFailureAggregation;
    }

    public String getErrorComponentType() {
        return errorComponentType;
    }

    public String getTransactionFailureComponentType() {
        return transactionFailureComponentType;
    }

    public NameFilter getStackTraceFilter() {
        return stackTraceFilter;
    }

    public List<ErrorAggregationStrategySchemaConfiguration> getErrorAggregationStrategies() {
        return errorAggregationStrategies;
    }

    public NameFilter getTransactionFailureFilter() {
        return transactionFailureFilter;
    }

    public boolean isTransactionFailureErrorLog() {
        return transactionFailureErrorLog;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackErrorLogSchemaConfiguration))
            return false;

        StackErrorLogSchemaConfiguration configuration = (StackErrorLogSchemaConfiguration) o;
        return super.equals(configuration) &&
                allowTypedErrorAggregation == configuration.allowTypedErrorAggregation &&
                allowTransactionFailureAggregation == configuration.allowTransactionFailureAggregation &&
                Objects.equals(errorComponentType, configuration.errorComponentType) &&
                Objects.equals(transactionFailureComponentType, configuration.transactionFailureComponentType) &&
                Objects.equals(stackTraceFilter, configuration.stackTraceFilter) &&
                errorAggregationStrategies.equals(configuration.errorAggregationStrategies) &&
                Objects.equals(transactionFailureFilter, configuration.transactionFailureFilter) &&
                transactionFailureErrorLog == configuration.transactionFailureErrorLog;
    }

    @Override
    public boolean equalsStructured(AggregationComponentTypeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof StackErrorLogSchemaConfiguration))
            return false;

        StackErrorLogSchemaConfiguration configuration = (StackErrorLogSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(allowTypedErrorAggregation, allowTransactionFailureAggregation,
                errorComponentType, transactionFailureComponentType, stackTraceFilter, errorAggregationStrategies,
                transactionFailureFilter, transactionFailureErrorLog);
    }

    private static LogSchemaConfiguration buildLogMetric(LogSchemaConfiguration metric,
                                                         boolean allowTypedErrorAggregation, String errorComponentType, NameFilter stackTraceFilter,
                                                         List<? extends ErrorAggregationStrategySchemaConfiguration> errorAggregationStrategies) {
        if (!allowTypedErrorAggregation)
            return metric;

        List<AggregationLogTransformerSchemaConfiguration> transformers =
                new ArrayList<AggregationLogTransformerSchemaConfiguration>(metric.getTransformers());
        transformers.add(new ErrorLogTransformerSchemaConfiguration(errorComponentType, stackTraceFilter, errorAggregationStrategies));
        return new LogSchemaConfiguration(metric.getName(), (List) metric.getRepresentations(), metric.getFilter(),
                transformers, metric.isFullTextIndex(), metric.getDocument());
    }
}
