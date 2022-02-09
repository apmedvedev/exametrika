/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.alerts;

import java.util.Map;

import com.exametrika.api.component.config.model.ExpressionComplexAlertSchemaConfiguration;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;
import com.exametrika.spi.component.IComplexAlertExpressionContext;
import com.exametrika.spi.component.IComplexRule;


/**
 * The {@link ExpressionComplexAlert} represents a expression complex alert.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionComplexAlert extends Alert implements IComplexRule {
    private final Map<String, Object> runtimeContext;
    private final IExpression onCondition;
    private final IExpression offCondition;
    private final Context context = new Context();

    public static class Context implements IComplexAlertExpressionContext {
        private IComponent component;
        private Map<String, Object> facts;

        public void setComponent(IComponent component) {
            this.component = component;
        }

        public void setFacts(Map<String, Object> facts) {
            this.facts = facts;
        }

        public void clear() {
            this.component = null;
            this.facts = null;
        }

        @Override
        public IComponent getComponent() {
            return component;
        }

        @Override
        public Map<String, Object> getFacts() {
            return facts;
        }

        @Override
        public boolean hasFact(String name) {
            return facts.containsKey(name);
        }

        @Override
        public Object fact(String name) {
            return facts.get(name);
        }
    }

    public ExpressionComplexAlert(ExpressionComplexAlertSchemaConfiguration configuration) {
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
    public void execute(IComponent c, Map<String, Object> facts) {
        ComponentNode component = (ComponentNode) c;
        IIncident incident = component.findIncident(getConfiguration().getName());

        context.setComponent(component);
        context.setFacts(facts);

        if (incident == null && onCondition.<Boolean>execute(context, runtimeContext))
            component.createIncident(this, false);
        else if (incident != null) {
            if (offCondition != null && offCondition.<Boolean>execute(context, runtimeContext))
                incident.delete(true);
            else if (offCondition == null && !onCondition.<Boolean>execute(context, runtimeContext))
                incident.delete(true);
        }

        context.clear();
    }
}
