/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.meters.config;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.common.meters.ExpressionLogProvider;
import com.exametrika.spi.aggregator.common.meters.ILogProvider;
import com.exametrika.spi.aggregator.common.meters.config.LogProviderConfiguration;


/**
 * The {@link ExpressionLogProviderConfiguration} is an expression log provider configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionLogProviderConfiguration extends LogProviderConfiguration {
    private final String expression;

    public ExpressionLogProviderConfiguration(String expression) {
        Assert.notNull(expression);

        this.expression = expression;
    }

    public final String getExpression() {
        return expression;
    }

    @Override
    public ILogProvider createProvider() {
        return new ExpressionLogProvider(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExpressionLogProviderConfiguration))
            return false;

        ExpressionLogProviderConfiguration configuration = (ExpressionLogProviderConfiguration) o;
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
