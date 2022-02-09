/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.alerts.ExpressionSimpleAlert;
import com.exametrika.spi.component.IAlert;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ExpressionSimpleAlertSchemaConfiguration} is an expression simple alert schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionSimpleAlertSchemaConfiguration extends AlertSchemaConfiguration {
    private final String onCondition;
    private final String offCondition;

    public ExpressionSimpleAlertSchemaConfiguration(String name, String description, List<? extends AlertChannelSchemaConfiguration> channels,
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
        return new ExpressionSimpleAlert(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExpressionSimpleAlertSchemaConfiguration))
            return false;

        ExpressionSimpleAlertSchemaConfiguration configuration = (ExpressionSimpleAlertSchemaConfiguration) o;
        return super.equals(configuration) && onCondition.equals(configuration.onCondition) &&
                Objects.equals(offCondition, configuration.offCondition);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(onCondition, offCondition);
    }
}
