/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import java.util.Map;

import com.exametrika.api.component.config.model.ExpressionGroupAvailabilityConditionSchemaConfiguration;
import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.api.component.nodes.IHealthComponentVersion;
import com.exametrika.api.component.nodes.IHealthComponentVersion.State;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IGroupComponentVersion;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.utils.ICondition;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;
import com.exametrika.spi.component.IGroupAvailabilityExpressionContext;


/**
 * The {@link ExpressionGroupAvailabilityCondition} represents a expression group availability condition.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionGroupAvailabilityCondition implements ICondition<IGroupComponent> {
    private final Map<String, Object> runtimeContext;
    private final IExpression condition;
    private final Context context = new Context();

    public static class Context implements IGroupAvailabilityExpressionContext {
        private IGroupComponent group;

        public void setGroup(IGroupComponent group) {
            this.group = group;
        }

        public void clear() {
            this.group = null;
        }

        @Override
        public IGroupComponent getGroup() {
            return group;
        }

        @Override
        public int getAvailable() {
            return getAvailableComponents() + getAvailableGroups();
        }

        @Override
        public int getAvailableComponents() {
            int count = 0;
            for (IComponent component : ((IGroupComponentVersion) group.getCurrentVersion()).getComponents()) {
                if (component instanceof IHealthComponent) {
                    IHealthComponentVersion version = (IHealthComponentVersion) component.getCurrentVersion();
                    State state = version.getState();
                    if (state == State.NORMAL || state == State.HEALTH_WARNING || state == State.MAINTENANCE)
                        count++;
                }

            }

            return count;
        }

        @Override
        public int getAvailableGroups() {
            int count = 0;
            for (IGroupComponent component : ((IGroupComponentVersion) group.getCurrentVersion()).getChildren()) {
                IGroupComponentVersion version = (IGroupComponentVersion) component.getCurrentVersion();
                State state = version.getState();
                if (state == State.NORMAL || state == State.HEALTH_WARNING || state == State.MAINTENANCE)
                    count++;
            }

            return count;
        }
    }

    public ExpressionGroupAvailabilityCondition(ExpressionGroupAvailabilityConditionSchemaConfiguration configuration) {
        CompileContext compileContext = Expressions.createCompileContext(null);
        runtimeContext = MeterExpressions.getRuntimeContext();
        this.condition = Expressions.compile(configuration.getExpression(), compileContext);
    }

    @Override
    public boolean evaluate(IGroupComponent group) {
        context.setGroup(group);
        boolean res = condition.execute(context, runtimeContext);
        context.clear();
        return res;
    }
}
