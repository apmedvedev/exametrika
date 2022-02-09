/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.rules;

import java.util.Collections;
import java.util.Map;

import com.exametrika.api.aggregator.fields.IAggregationField;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.schema.IAggregationFieldSchema;
import com.exametrika.api.component.config.model.ExpressionSimpleRuleSchemaConfiguration;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.values.MeasurementExpressionContext;
import com.exametrika.spi.aggregator.IRuleContext;
import com.exametrika.spi.aggregator.IRuleExecutor;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;
import com.exametrika.spi.component.ISimpleRule;
import com.exametrika.spi.component.ISimpleRuleExpressionContext;


/**
 * The {@link ExpressionSimpleRule} is a simple rule.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionSimpleRule implements ISimpleRule {
    private final ExpressionSimpleRuleSchemaConfiguration configuration;
    private final Map<String, Object> runtimeContext;
    private final IExpression expression;
    private final Context valueContext = new Context();

    public static class Context extends MeasurementExpressionContext implements ISimpleRuleExpressionContext {
        private IComponent component;
        private IAggregationNode measurement;
        private IRuleContext context;

        public void setComponent(IComponent component) {
            this.component = component;
        }

        public void setMeasurement(IAggregationNode measurement) {
            this.measurement = measurement;
        }

        public void setRuleContext(IRuleContext context) {
            this.context = context;
        }

        @Override
        public void clear() {
            super.clear();

            this.component = null;
            this.measurement = null;
            this.context = null;
        }

        @Override
        public IComponent getComponent() {
            return component;
        }

        @Override
        public IAggregationNode getMeasurement() {
            return measurement;
        }

        @Override
        public Map<String, Object> getFacts() {
            if (context != null)
                return context.getFacts((IRuleExecutor) component);
            else
                return Collections.emptyMap();
        }

        @Override
        public boolean hasFact(String name) {
            if (context != null)
                return context.getFacts((IRuleExecutor) component).containsKey(name);
            else
                return false;
        }

        @Override
        public Object fact(String name) {
            if (context != null)
                return context.getFacts((IRuleExecutor) component).get(name);
            else
                return null;
        }

        @Override
        public void fact(String name, Object value) {
            if (context != null)
                context.setFact((IRuleExecutor) component, name, value);
        }

        @Override
        public void addFact(String name, Object value) {
            if (context != null)
                context.addFact((IRuleExecutor) component, name, value);
        }

        @Override
        public void incFact(String name) {
            if (context != null)
                context.incrementFact((IRuleExecutor) component, name);
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

    public ExpressionSimpleRule(ExpressionSimpleRuleSchemaConfiguration configuration) {
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
    public void execute(IComponent component, IAggregationNode node, IRuleContext context) {
        IAggregationField aggregationField = node.getAggregationField();
        IAggregationFieldSchema aggregationFieldSchema = aggregationField.getSchema();

        valueContext.setComponent(component);
        valueContext.setMeasurement(node);
        valueContext.setRuleContext(context);
        valueContext.setValue(aggregationField.getValue(false));
        valueContext.setComputeContext(aggregationField.getComputeContext());
        valueContext.setComponentAccessorFactory(aggregationFieldSchema.getRuleRepresentation().getAccessorFactory());

        expression.execute(valueContext, runtimeContext);

        valueContext.clear();
    }
}
