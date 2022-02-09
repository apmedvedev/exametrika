/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.jobs.config.model.SchedulePeriodSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;


/**
 * The {@link MailAlertChannelSchemaConfiguration} is an mail alert channel schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class MailAlertChannelSchemaConfiguration extends AlertChannelSchemaConfiguration {
    private final String onSubject;
    private final String offSubject;
    private final String statusSubject;
    private final boolean formatted;
    private final String senderName;
    private final String senderAddress;

    public MailAlertChannelSchemaConfiguration(String name, String onTemplate, String offTemplate, String statusTemplate,
                                               ScheduleSchemaConfiguration schedule, SchedulePeriodSchemaConfiguration period, List<AlertRecipientSchemaConfiguration> recipients,
                                               String onSubject, String offSubject, String statusSubject, boolean formatted, String senderName, String senderAddress) {
        super(name, onTemplate, offTemplate, statusTemplate, schedule, period, recipients);

        Assert.notNull(onSubject);
        if (offSubject == null)
            offSubject = onSubject;
        if (statusSubject == null)
            statusSubject = onSubject;
        Assert.isTrue((senderName == null) == (senderAddress == null));

        this.onSubject = onSubject;
        this.offSubject = offSubject;
        this.statusSubject = statusSubject;
        this.formatted = formatted;
        this.senderName = senderName;
        this.senderAddress = senderAddress;
    }

    public String getOnSubject() {
        return onSubject;
    }

    public String getOffSubject() {
        return offSubject;
    }

    public String getStatusSubject() {
        return statusSubject;
    }

    public boolean isFormatted() {
        return formatted;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MailAlertChannelSchemaConfiguration))
            return false;

        MailAlertChannelSchemaConfiguration configuration = (MailAlertChannelSchemaConfiguration) o;
        return super.equals(configuration) && onSubject.equals(configuration.onSubject) && offSubject.equals(configuration.offSubject) &&
                statusSubject.equals(configuration.statusSubject) && formatted == configuration.formatted &&
                Objects.equals(senderName, configuration.senderName) && Objects.equals(senderAddress, configuration.senderAddress);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(onSubject, offSubject, statusSubject, formatted, senderName, senderAddress);
    }
}
