/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component.schema;

import com.exametrika.api.component.config.model.AlertChannelSchemaConfiguration;
import com.exametrika.common.expression.ITemplate;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.ISchedulePeriod;


/**
 * The {@link IAlertChannelSchema} represents a component alert channel schema.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IAlertChannelSchema {
    /**
     * Returns alert channel schema configuration.
     *
     * @return alert channel schema configuration
     */
    AlertChannelSchemaConfiguration getConfiguration();

    /**
     * Returns on template.
     *
     * @return on template
     */
    ITemplate getOnTemplate();

    /**
     * Returns off template.
     *
     * @return off template
     */
    ITemplate getOffTemplate();

    /**
     * Returns status template.
     *
     * @return status template
     */
    ITemplate getStatusTemplate();

    /**
     * Returns schedule.
     *
     * @return schedule
     */
    ISchedule getSchedule();

    /**
     * Returns period.
     *
     * @return period
     */
    ISchedulePeriod getPeriod();
}
