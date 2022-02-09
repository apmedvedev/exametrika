/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.util.Locale;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.jobs.schedule.LowMemorySchedule;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link LowMemoryScheduleSchemaConfiguration} represents a configuration of low memory schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class LowMemoryScheduleSchemaConfiguration extends ScheduleSchemaConfiguration {
    private final long minFreeSpace;
    private final boolean included;

    public LowMemoryScheduleSchemaConfiguration(long minFreeSpace, boolean included) {
        Assert.isTrue(minFreeSpace >= 0);

        this.minFreeSpace = minFreeSpace;
        this.included = included;
    }

    public long getMinFreeSpace() {
        return minFreeSpace;
    }

    public boolean isIncluded() {
        return included;
    }

    @Override
    public ISchedule createSchedule() {
        return new LowMemorySchedule(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LowMemoryScheduleSchemaConfiguration))
            return false;

        LowMemoryScheduleSchemaConfiguration configuration = (LowMemoryScheduleSchemaConfiguration) o;
        return minFreeSpace == configuration.minFreeSpace && included == configuration.included;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(minFreeSpace, included);
    }

    @Override
    public String toString() {
        return toString(Locale.getDefault());
    }

    @Override
    public String toString(Locale locale) {
        return "lowMemory" + (included ? "" : "-") + "(" + minFreeSpace + ")";
    }
}