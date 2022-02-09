/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.meters.config;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.common.meters.ExpressionLogFilter;
import com.exametrika.spi.aggregator.common.meters.ILogFilter;
import com.exametrika.spi.aggregator.common.meters.config.LogFilterConfiguration;


/**
 * The {@link ExpressionLogFilterConfiguration} is an expression log filter configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionLogFilterConfiguration extends LogFilterConfiguration {
    private final String expression;

    public ExpressionLogFilterConfiguration(String expression) {
        Assert.notNull(expression);

        this.expression = expression;
    }

    public final String getExpression() {
        return expression;
    }

    @Override
    public ILogFilter createFilter() {
        return new ExpressionLogFilter(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExpressionLogFilterConfiguration))
            return false;

        ExpressionLogFilterConfiguration configuration = (ExpressionLogFilterConfiguration) o;
        return expression.equals(configuration.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(expression);
    }

    @Override
    public String toString() {
        return expression;
    }
}
