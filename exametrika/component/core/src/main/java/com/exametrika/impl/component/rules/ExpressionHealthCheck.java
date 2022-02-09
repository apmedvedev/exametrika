/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.rules;

import java.util.Map;

import com.exametrika.api.component.config.model.ExpressionHealthCheckSchemaConfiguration;
import com.exametrika.api.component.nodes.IHealthComponentVersion.State;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;
import com.exametrika.spi.component.IHealthCheck;
import com.exametrika.spi.component.IHealthCheckExpressionContext;


/**
 * The {@link ExpressionHealthCheck} is a health check.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionHealthCheck implements IHealthCheck {
    private final ExpressionHealthCheckSchemaConfiguration configuration;
    private final Map<String, Object> runtimeContext;
    private final IExpression expression;
    private final Context context = new Context();

    public static class Context implements IHealthCheckExpressionContext {
        private IComponent component;
        private State oldState;
        private State newState;

        public void setComponent(IComponent component) {
            this.component = component;
        }

        public void setOldState(State state) {
            oldState = state;
        }

        public void setNewState(State state) {
            newState = state;
        }

        public void clear() {
            component = null;
            oldState = null;
            newState = null;
        }

        @Override
        public IComponent getComponent() {
            return component;
        }

        @Override
        public String getOldState() {
            return oldState.toString().toLowerCase();
        }

        @Override
        public String getNewState() {
            return newState.toString().toLowerCase();
        }

        @Override
        public void action(String name, Map<String, ?> parameters) {
            component.createAction(name).execute(parameters);
        }

        @Override
        public void action(IComponent component, String name, Map<String, ?> parameters) {
            component.createAction(name).execute(parameters);
        }
    }

    public ExpressionHealthCheck(ExpressionHealthCheckSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        CompileContext compileContext = Expressions.createCompileContext(null);
        runtimeContext = MeterExpressions.getRuntimeContext();
        this.expression = Expressions.compile(configuration.getExpression(), compileContext);
    }

    @Override
    public String getName() {
        return configuration.getName();
    }

    @Override
    public void onStateChanged(IComponent component, State oldState, State newState) {
        context.setComponent(component);
        context.setOldState(oldState);
        context.setNewState(newState);

        expression.execute(context, runtimeContext);

        context.clear();
    }
}
