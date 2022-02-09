/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.List;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.jobs.config.model.SchedulePeriodSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;


/**
 * The {@link AlertChannelSchemaConfiguration} is an alert channel schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AlertChannelSchemaConfiguration extends Configuration {
    private final String name;
    private final String onTemplate;
    private final String offTemplate;
    private final String statusTemplate;
    private final ScheduleSchemaConfiguration schedule;
    private final SchedulePeriodSchemaConfiguration period;
    private final List<AlertRecipientSchemaConfiguration> recipients;

    public AlertChannelSchemaConfiguration(String name, String onTemplate, String offTemplate, String statusTemplate,
                                           ScheduleSchemaConfiguration schedule, SchedulePeriodSchemaConfiguration period, List<AlertRecipientSchemaConfiguration> recipients) {
        Assert.notNull(name);
        Assert.notNull(onTemplate);
        if (schedule != null)
            Assert.notNull(period);
        Assert.notNull(recipients);

        this.name = name;
        this.onTemplate = onTemplate;
        this.offTemplate = offTemplate;
        this.statusTemplate = statusTemplate;
        this.schedule = schedule;
        this.period = period;
        this.recipients = Immutables.wrap(recipients);
    }

    public String getName() {
        return name;
    }

    public String getOnTemplate() {
        return onTemplate;
    }

    public String getOffTemplate() {
        return offTemplate;
    }

    public String getStatusTemplate() {
        return statusTemplate;
    }

    public ScheduleSchemaConfiguration getSchedule() {
        return schedule;
    }

    public SchedulePeriodSchemaConfiguration getPeriod() {
        return period;
    }

    public List<AlertRecipientSchemaConfiguration> getRecipients() {
        return recipients;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AlertChannelSchemaConfiguration))
            return false;

        AlertChannelSchemaConfiguration configuration = (AlertChannelSchemaConfiguration) o;
        return name.equals(configuration.name) && onTemplate.equals(configuration.onTemplate) &&
                Objects.equals(offTemplate, configuration.offTemplate) && Objects.equals(statusTemplate, configuration.statusTemplate) &&
                Objects.equals(schedule, configuration.schedule) && Objects.equals(period, configuration.period) &&
                recipients.equals(configuration.recipients);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, onTemplate, offTemplate, statusTemplate, schedule, period, recipients);
    }

    @Override
    public String toString() {
        return name;
    }
}
