/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.alerts.ExpressionIncidentGroupAlert;
import com.exametrika.spi.component.IAlert;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ExpressionIncidentGroupSchemaConfiguration} is an expression incident group schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionIncidentGroupSchemaConfiguration extends AlertSchemaConfiguration {
    private final String expression;

    public ExpressionIncidentGroupSchemaConfiguration(String name, String description, List<? extends AlertChannelSchemaConfiguration> channels,
                                                      List<String> tags, boolean enabled, String expression) {
        super(name, description, channels, tags, enabled);

        Assert.notNull(expression);

        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public IAlert createAlert(IDatabaseContext context) {
        return new ExpressionIncidentGroupAlert(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExpressionIncidentGroupSchemaConfiguration))
            return false;

        ExpressionIncidentGroupSchemaConfiguration configuration = (ExpressionIncidentGroupSchemaConfiguration) o;
        return super.equals(configuration) && expression.equals(configuration.expression);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(expression);
    }
}
