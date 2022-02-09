/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.util.Locale;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.jobs.schedule.LowDiskSchedule;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link LowDiskScheduleSchemaConfiguration} represents a configuration of low disk schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class LowDiskScheduleSchemaConfiguration extends ScheduleSchemaConfiguration {
    private final String path;
    private final long minFreeSpace;
    private final boolean included;

    public LowDiskScheduleSchemaConfiguration(String path, long minFreeSpace, boolean included) {
        Assert.notNull(path);
        Assert.isTrue(minFreeSpace >= 0);

        this.path = path;
        this.minFreeSpace = minFreeSpace;
        this.included = included;
    }

    public String getPath() {
        return path;
    }

    public long getMinFreeSpace() {
        return minFreeSpace;
    }

    public boolean isIncluded() {
        return included;
    }

    @Override
    public ISchedule createSchedule() {
        return new LowDiskSchedule(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LowDiskScheduleSchemaConfiguration))
            return false;

        LowDiskScheduleSchemaConfiguration configuration = (LowDiskScheduleSchemaConfiguration) o;
        return path.equals(configuration.path) && minFreeSpace == configuration.minFreeSpace && included == configuration.included;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path, minFreeSpace, included);
    }

    @Override
    public String toString() {
        return toString(Locale.getDefault());
    }

    @Override
    public String toString(Locale locale) {
        return "lowDisk" + (included ? "" : "-") + "(" + path + "," + minFreeSpace + ")";
    }
}