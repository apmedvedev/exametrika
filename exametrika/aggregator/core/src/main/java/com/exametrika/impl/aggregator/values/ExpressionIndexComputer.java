/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import java.util.Map;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.values.ObjectBuilder;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;


/**
 * The {@link ExpressionIndexComputer} is an computer of expression index metric type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionIndexComputer extends ObjectComputer {
    private final IComponentAccessorFactory componentAccessorFactory;
    private final boolean stored;
    private final Map<String, Object> runtimeContext;
    private final IExpression expression;
    private final MeasurementExpressionContext expressionContext = new MeasurementExpressionContext();

    public ExpressionIndexComputer(boolean stored, String expression, IComponentAccessorFactory componentAccessorFactory) {
        this.componentAccessorFactory = componentAccessorFactory;
        Assert.notNull(expression);
        Assert.notNull(componentAccessorFactory);

        this.stored = stored;

        CompileContext compileContext = Expressions.createCompileContext(null);
        runtimeContext = MeterExpressions.getRuntimeContext();
        this.expression = Expressions.compile(expression, compileContext);
    }

    @Override
    public Object compute(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
        if (stored) {
            if (value != null)
                return ((IObjectValue) value).getObject();
            else
                return null;
        } else
            return compute(componentValue, context);
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
        if (stored) {
            if (value != null) {
                ObjectBuilder builder = (ObjectBuilder) value;
                builder.setObject(compute(componentValue, context));
            }
        }
    }

    private Object compute(IComponentValue componentValue, IComputeContext context) {
        expressionContext.setValue(componentValue);
        expressionContext.setComputeContext(context);
        expressionContext.setComponentAccessorFactory(componentAccessorFactory);

        Object result = expression.execute(expressionContext, runtimeContext);

        expressionContext.clear();
        return result;
    }
}
