/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.jobs.ExpressionComponentJobOperation;
import com.exametrika.spi.component.config.model.ComponentJobOperationSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.IJobContext;


/**
 * The {@link ExpressionComponentJobOperationSchemaConfiguration} is an expression component job operation schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExpressionComponentJobOperationSchemaConfiguration extends ComponentJobOperationSchemaConfiguration {
    private final String expression;

    public ExpressionComponentJobOperationSchemaConfiguration(String expression) {
        Assert.notNull(expression);

        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public Runnable createOperation(IJobContext context) {
        return new ExpressionComponentJobOperation(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExpressionComponentJobOperationSchemaConfiguration))
            return false;

        ExpressionComponentJobOperationSchemaConfiguration configuration = (ExpressionComponentJobOperationSchemaConfiguration) o;
        return expression.equals(configuration.expression);
    }

    @Override
    public int hashCode() {
        return expression.hashCode();
    }
}
