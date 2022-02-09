/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.ErrorLogTransformer;
import com.exametrika.spi.aggregator.IAggregationLogTransformer;
import com.exametrika.spi.aggregator.config.model.AggregationLogTransformerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ErrorAggregationStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ErrorLogTransformerSchemaConfiguration} is a error log transformer schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ErrorLogTransformerSchemaConfiguration extends AggregationLogTransformerSchemaConfiguration {
    private final String errorComponentType;
    private final NameFilter stackTraceFilter;
    private final List<ErrorAggregationStrategySchemaConfiguration> errorAggregationStrategies;

    public ErrorLogTransformerSchemaConfiguration(String errorComponentType, NameFilter stackTraceFilter,
                                                  List<? extends ErrorAggregationStrategySchemaConfiguration> errorAggregationStrategies) {
        Assert.notNull(errorComponentType);
        Assert.notNull(errorAggregationStrategies);

        this.errorComponentType = errorComponentType;
        this.stackTraceFilter = stackTraceFilter;
        this.errorAggregationStrategies = Immutables.wrap(errorAggregationStrategies);
    }

    public String getErrorComponentType() {
        return errorComponentType;
    }

    public NameFilter getStackTraceFilter() {
        return stackTraceFilter;
    }

    public List<ErrorAggregationStrategySchemaConfiguration> getErrorAggregationStrategies() {
        return errorAggregationStrategies;
    }

    @Override
    public IAggregationLogTransformer createTransformer(IDatabaseContext context) {
        return new ErrorLogTransformer(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ErrorLogTransformerSchemaConfiguration))
            return false;

        ErrorLogTransformerSchemaConfiguration configuration = (ErrorLogTransformerSchemaConfiguration) o;
        return Objects.equals(errorComponentType, configuration.errorComponentType) &&
                Objects.equals(stackTraceFilter, configuration.stackTraceFilter) &&
                errorAggregationStrategies.equals(configuration.errorAggregationStrategies);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(errorComponentType, stackTraceFilter, errorAggregationStrategies);
    }
}
