/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.monitors;

import java.util.Map;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;
import com.exametrika.spi.metrics.host.IProcessContext;
import com.exametrika.spi.metrics.host.IProcessNamingStrategy;


/**
 * The {@link ExpressionProcessNamingStrategy} is an expression based process naming strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionProcessNamingStrategy implements IProcessNamingStrategy {
    private final IExpression expression;
    private final Map<String, Object> runtimeContext;

    public ExpressionProcessNamingStrategy(String expression) {
        Assert.notNull(expression);

        CompileContext compileContext = Expressions.createCompileContext(null);
        this.expression = Expressions.compile(expression, compileContext);
        runtimeContext = MeterExpressions.getRuntimeContext();
    }

    @Override
    public String getName(IProcessContext context) {
        return expression.execute(context, runtimeContext);
    }
}
