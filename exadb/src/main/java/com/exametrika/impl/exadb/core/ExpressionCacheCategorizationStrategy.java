/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core;

import java.util.List;
import java.util.Map;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.exadb.core.ICacheCategorizationStrategy;


/**
 * The {@link ExpressionCacheCategorizationStrategy} is an expression based cache categorization strategy strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionCacheCategorizationStrategy implements ICacheCategorizationStrategy {
    private final IExpression expression;
    private final Map<String, Object> runtimeContext;

    public ExpressionCacheCategorizationStrategy(String expression) {
        Assert.notNull(expression);

        CompileContext compileContext = Expressions.createCompileContext(null);
        this.expression = Expressions.compile(expression, compileContext);
        runtimeContext = Expressions.createRuntimeContext(null, true);
    }

    @Override
    public Pair<String, String> categorize(Map<String, String> cacheElementProperties) {
        List<String> list = expression.execute(cacheElementProperties, runtimeContext);
        Assert.checkState(list.size() == 2);
        return new Pair<String, String>(list.get(0), list.get(1));
    }
}
