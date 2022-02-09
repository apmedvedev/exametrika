/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.alerts;

import java.util.Map;

import com.exametrika.api.aggregator.fields.IAggregationField;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.schema.IAggregationFieldSchema;
import com.exametrika.api.component.config.model.ExpressionSimpleAlertSchemaConfiguration;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.impl.aggregator.values.MeasurementExpressionContext;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.spi.aggregator.IRuleContext;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;
import com.exametrika.spi.component.ISimpleAlertExpressionContext;
import com.exametrika.spi.component.ISimpleRule;


/**
 * The {@link ExpressionSimpleAlert} represents a expression simple alert.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionSimpleAlert extends Alert implements ISimpleRule {
    private final Map<String, Object> runtimeContext;
    private final IExpression onCondition;
    private final IExpression offCondition;
    private final Context valueContext = new Context();

    public static class Context extends MeasurementExpressionContext implements ISimpleAlertExpressionContext {
        private IComponent component;
        private IAggregationNode measurement;

        public void setComponent(IComponent component) {
            this.component = component;
        }

        public void setMeasurement(IAggregationNode measurement) {
            this.measurement = measurement;
        }

        @Override
        public void clear() {
            super.clear();

            this.component = null;
            this.measurement = null;
        }

        @Override
        public IComponent getComponent() {
            return component;
        }

        @Override
        public IAggregationNode getMeasurement() {
            return measurement;
        }
    }

    public ExpressionSimpleAlert(ExpressionSimpleAlertSchemaConfiguration configuration) {
        super(configuration);

        CompileContext compileContext = Expressions.createCompileContext(null);
        runtimeContext = MeterExpressions.getRuntimeContext();
        this.onCondition = Expressions.compile(configuration.getOnCondition(), compileContext);

        if (configuration.getOffCondition() != null)
            this.offCondition = Expressions.compile(configuration.getOffCondition(), compileContext);
        else
            this.offCondition = null;
    }

    @Override
    public String getName() {
        return getConfiguration().getName();
    }

    @Override
    public void execute(IComponent c, IAggregationNode node, IRuleContext context) {
        ComponentNode component = (ComponentNode) c;
        IIncident incident = component.findIncident(getConfiguration().getName());

        IAggregationField aggregationField = node.getAggregationField();
        IAggregationFieldSchema aggregationFieldSchema = aggregationField.getSchema();

        valueContext.setComponent(component);
        valueContext.setMeasurement(node);
        valueContext.setValue(aggregationField.getValue(false));
        valueContext.setComputeContext(aggregationField.getComputeContext());
        valueContext.setComponentAccessorFactory(aggregationFieldSchema.getRuleRepresentation().getAccessorFactory());

        if (incident == null && onCondition.<Boolean>execute(valueContext, runtimeContext))
            component.createIncident(this, false);
        else if (incident != null) {
            if (offCondition != null && offCondition.<Boolean>execute(valueContext, runtimeContext))
                incident.delete(true);
            else if (offCondition == null && !onCondition.<Boolean>execute(valueContext, runtimeContext))
                incident.delete(true);
        }

        valueContext.clear();
    }
}
