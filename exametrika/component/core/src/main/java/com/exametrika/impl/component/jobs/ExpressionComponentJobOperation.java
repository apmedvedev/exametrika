/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.jobs;

import java.util.Map;

import com.exametrika.api.component.IAction;
import com.exametrika.api.component.IAsyncAction;
import com.exametrika.api.component.config.model.ExpressionComponentJobOperationSchemaConfiguration;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;
import com.exametrika.spi.component.IComponentJobOperationExpressionContext;
import com.exametrika.spi.exadb.jobs.IJobContext;


/**
 * The {@link ExpressionComponentJobOperation} is a exprssion component job operation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExpressionComponentJobOperation extends ComponentJobOperation {
    private final Map<String, Object> runtimeContext;
    private final IExpression expression;
    private final Context context = new Context();

    public static class Context implements IComponentJobOperationExpressionContext {
        private IComponent component;
        private IJobContext jobContext;
        private ExpressionComponentJobOperation operation;

        public void setComponent(IComponent component) {
            this.component = component;
        }

        public void setJobContext(IJobContext jobContext) {
            this.jobContext = jobContext;
        }

        public void setOperation(ExpressionComponentJobOperation operation) {
            this.operation = operation;
        }

        public void clear() {
            component = null;
            jobContext = null;
            operation = null;
        }

        @Override
        public IComponent getComponent() {
            return component;
        }

        @Override
        public JobSchemaConfiguration getSchema() {
            return jobContext.getSchema();
        }

        @Override
        public boolean isPredefined() {
            return jobContext.isPredefined();
        }

        @Override
        public void action(String name, Map<String, ?> parameters) {
            IAction action = component.createAction(name);
            if (action.getSchema().getConfiguration().isAsync()) {
                operation.async = true;
                ((IAsyncAction) action).execute(parameters, operation);
            } else
                action.execute(parameters);
        }
    }

    public ExpressionComponentJobOperation(ExpressionComponentJobOperationSchemaConfiguration configuration, IJobContext context) {
        super(context);

        Assert.notNull(configuration);

        CompileContext compileContext = Expressions.createCompileContext(null);
        runtimeContext = MeterExpressions.getRuntimeContext();
        this.expression = Expressions.compile(configuration.getExpression(), compileContext);
    }

    @Override
    protected void execute(IJobContext jobContext, IComponent component) {
        context.setComponent(component);
        context.setJobContext(jobContext);
        context.setOperation(this);

        expression.execute(context, runtimeContext);

        context.clear();
    }
}
