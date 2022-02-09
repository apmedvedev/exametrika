/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.rules.ExpressionHealthCheck;
import com.exametrika.spi.component.IRule;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ExpressionHealthCheckSchemaConfiguration} is an expression health check schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionHealthCheckSchemaConfiguration extends RuleSchemaConfiguration {
    private final String expression;

    public ExpressionHealthCheckSchemaConfiguration(String name, String expression, boolean enabled) {
        super(name, enabled);

        Assert.notNull(expression);

        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public IRule createRule(IDatabaseContext context) {
        return new ExpressionHealthCheck(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExpressionHealthCheckSchemaConfiguration))
            return false;

        ExpressionHealthCheckSchemaConfiguration configuration = (ExpressionHealthCheckSchemaConfiguration) o;
        return super.equals(configuration) && expression.equals(configuration.expression);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(expression);
    }
}
