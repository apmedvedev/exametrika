/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;

import java.util.Map;

import com.exametrika.common.expression.Expressions;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.aggregator.common.meters.StandardExpressionContext;


/**
 * The {@link MeterExpressions} contains various utility methods for work with expressions in profiler.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class MeterExpressions {
    private static volatile Map<String, Object> runtimeContext;

    public static Map<String, Object> getRuntimeContext() {
        if (runtimeContext != null)
            return runtimeContext;
        else
            return createRuntimeContext();
    }

    private static synchronized Map<String, Object> createRuntimeContext() {
        if (runtimeContext != null)
            return runtimeContext;

        Map<String, Object> runtimeContext = Expressions.createRuntimeContext(null, false);
        runtimeContext.put("exa", new StandardExpressionContext());
        MeterExpressions.runtimeContext = Immutables.wrap(runtimeContext);

        return MeterExpressions.runtimeContext;
    }

    private MeterExpressions() {
    }
}