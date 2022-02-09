/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.meters;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.api.aggregator.common.meters.config.ExpressionLogProviderConfiguration;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.spi.aggregator.common.meters.ILogEvent;
import com.exametrika.spi.aggregator.common.meters.ILogProvider;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;


/**
 * The {@link ExpressionLogProvider} is an expression log provider implementation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ExpressionLogProvider implements ILogProvider {
    private final IExpression expression;
    private final Map<String, Object> runtimeContext;

    public ExpressionLogProvider(ExpressionLogProviderConfiguration configuration) {
        CompileContext compileContext = Expressions.createCompileContext(null);
        runtimeContext = MeterExpressions.getRuntimeContext();
        this.expression = Expressions.compile(configuration.getExpression(), compileContext);
    }

    @Override
    public Object getValue(ILogEvent value) {
        return expression.execute(value, createRuntimeContext(value.getParameters()));
    }

    private Map<String, Object> createRuntimeContext(Map<String, Object> parameters) {
        Map<String, Object> map = new LinkedHashMap<String, Object>(parameters);
        map.putAll(runtimeContext);

        return map;
    }
}
