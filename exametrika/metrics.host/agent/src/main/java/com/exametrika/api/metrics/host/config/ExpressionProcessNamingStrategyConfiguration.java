/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.host.config;

import com.exametrika.common.utils.Assert;
import com.exametrika.impl.metrics.host.monitors.ExpressionProcessNamingStrategy;
import com.exametrika.spi.metrics.host.IProcessNamingStrategy;
import com.exametrika.spi.metrics.host.ProcessNamingStrategyConfiguration;


/**
 * The {@link ExpressionProcessNamingStrategyConfiguration} is a configuration for expression based process naming strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionProcessNamingStrategyConfiguration extends ProcessNamingStrategyConfiguration {
    private final String expression;

    public ExpressionProcessNamingStrategyConfiguration(String expression) {
        Assert.notNull(expression);

        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public IProcessNamingStrategy createStrategy() {
        return new ExpressionProcessNamingStrategy(expression);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExpressionProcessNamingStrategyConfiguration))
            return false;

        ExpressionProcessNamingStrategyConfiguration configuration = (ExpressionProcessNamingStrategyConfiguration) o;
        return expression.equals(configuration.expression);
    }

    @Override
    public int hashCode() {
        return expression.hashCode();
    }

    @Override
    public String toString() {
        return expression;
    }
}
