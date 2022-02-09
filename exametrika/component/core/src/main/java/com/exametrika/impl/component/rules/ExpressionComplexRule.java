/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.rules;

import java.util.Map;

import com.exametrika.api.component.config.model.ExpressionComplexRuleSchemaConfiguration;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;
import com.exametrika.spi.component.IComplexRule;
import com.exametrika.spi.component.IComplexRuleExpressionContext;


/**
 * The {@link ExpressionComplexRule} is a complex rule.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionComplexRule implements IComplexRule {
    private final ExpressionComplexRuleSchemaConfiguration configuration;
    private final Map<String, Object> runtimeContext;
    private final IExpression expression;
    private final Context context = new Context();

    public static class Context implements IComplexRuleExpressionContext {
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

        @Override
        public void action(String name, Map<String, ?> parameters) {
            component.createAction(name).execute(parameters);
        }

        @Override
        public void action(IComponent component, String name, Map<String, ?> parameters) {
            component.createAction(name).execute(parameters);
        }
    }

    public ExpressionComplexRule(ExpressionComplexRuleSchemaConfiguration configuration) {
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
    public void execute(IComponent component, Map<String, Object> facts) {
        context.setComponent(component);
        context.setFacts(facts);

        expression.execute(context, runtimeContext);

        context.clear();
    }
}
