/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import com.exametrika.api.component.config.model.AlertChannelSchemaConfiguration;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.ITemplate;
import com.exametrika.common.expression.Templates;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.component.schema.IAlertChannelSchema;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.ISchedulePeriod;


/**
 * The {@link AlertChannelSchema} represents an alert channel schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AlertChannelSchema implements IAlertChannelSchema {
    private final AlertChannelSchemaConfiguration configuration;
    private final ITemplate onTemplate;
    private final ITemplate offTemplate;
    private final ITemplate statusTemplate;
    private final ISchedule schedule;
    private final ISchedulePeriod period;

    public AlertChannelSchema(AlertChannelSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        CompileContext compileContext = Expressions.createCompileContext(null);

        onTemplate = Templates.compile(configuration.getOnTemplate(), compileContext, null);

        if (configuration.getOffTemplate() != null)
            offTemplate = Templates.compile(configuration.getOffTemplate(), compileContext, null);
        else
            offTemplate = null;

        if (configuration.getStatusTemplate() != null)
            statusTemplate = Templates.compile(configuration.getStatusTemplate(), compileContext, null);
        else
            statusTemplate = null;

        if (configuration.getSchedule() != null)
            schedule = configuration.getSchedule().createSchedule();
        else
            schedule = null;

        if (configuration.getPeriod() != null)
            period = configuration.getPeriod().createPeriod();
        else
            period = null;
    }

    @Override
    public AlertChannelSchemaConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public ITemplate getOnTemplate() {
        return onTemplate;
    }

    @Override
    public ITemplate getOffTemplate() {
        return offTemplate;
    }

    @Override
    public ITemplate getStatusTemplate() {
        return statusTemplate;
    }

    @Override
    public ISchedule getSchedule() {
        return schedule;
    }

    @Override
    public ISchedulePeriod getPeriod() {
        return period;
    }
}
