/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.nodes.ExpressionGroupAvailabilityCondition;
import com.exametrika.spi.component.config.model.GroupAvailabilityConditionSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ExpressionGroupAvailabilityConditionSchemaConfiguration} is a pattern group discovery strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionGroupAvailabilityConditionSchemaConfiguration extends GroupAvailabilityConditionSchemaConfiguration {
    private final String expression;

    public ExpressionGroupAvailabilityConditionSchemaConfiguration(String expression) {
        Assert.notNull(expression);

        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public ICondition<IGroupComponent> createCondition(IDatabaseContext context) {
        return new ExpressionGroupAvailabilityCondition(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExpressionGroupAvailabilityConditionSchemaConfiguration))
            return false;

        ExpressionGroupAvailabilityConditionSchemaConfiguration configuration = (ExpressionGroupAvailabilityConditionSchemaConfiguration) o;
        return expression.equals(configuration.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(expression);
    }
}
