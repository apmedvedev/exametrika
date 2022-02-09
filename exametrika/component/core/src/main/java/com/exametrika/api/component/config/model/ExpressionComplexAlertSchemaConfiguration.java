/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.alerts.ExpressionComplexAlert;
import com.exametrika.spi.component.IAlert;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ExpressionComplexAlertSchemaConfiguration} is an expression simple alert schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionComplexAlertSchemaConfiguration extends AlertSchemaConfiguration {
    private final String onCondition;
    private final String offCondition;

    public ExpressionComplexAlertSchemaConfiguration(String name, String description, List<? extends AlertChannelSchemaConfiguration> channels,
                                                     List<String> tags, boolean enabled, String onCondition, String offCondition) {
        super(name, description, channels, tags, enabled);

        Assert.notNull(onCondition);

        this.onCondition = onCondition;
        this.offCondition = offCondition;
    }

    public String getOnCondition() {
        return onCondition;
    }

    public String getOffCondition() {
        return offCondition;
    }

    @Override
    public IAlert createAlert(IDatabaseContext context) {
        return new ExpressionComplexAlert(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExpressionComplexAlertSchemaConfiguration))
            return false;

        ExpressionComplexAlertSchemaConfiguration configuration = (ExpressionComplexAlertSchemaConfiguration) o;
        return super.equals(configuration) && onCondition.equals(configuration.onCondition) &&
                Objects.equals(offCondition, configuration.offCondition);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(onCondition, offCondition);
    }
}
