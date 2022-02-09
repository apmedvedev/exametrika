/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.config;

import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.ExpressionCacheCategorizationStrategy;
import com.exametrika.spi.exadb.core.ICacheCategorizationStrategy;
import com.exametrika.spi.exadb.core.config.CacheCategorizationStrategyConfiguration;


/**
 * The {@link ExpressionCacheCategorizationStrategyConfiguration} is a null (no-op) archive store configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionCacheCategorizationStrategyConfiguration extends CacheCategorizationStrategyConfiguration {
    private final String expression;

    public ExpressionCacheCategorizationStrategyConfiguration(String expression) {
        Assert.notNull(expression);

        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public ICacheCategorizationStrategy createStrategy() {
        return new ExpressionCacheCategorizationStrategy(expression);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExpressionCacheCategorizationStrategyConfiguration))
            return false;

        ExpressionCacheCategorizationStrategyConfiguration configuration = (ExpressionCacheCategorizationStrategyConfiguration) o;
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
