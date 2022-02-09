/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link ProfilerRecorderConfiguration} is a configuration of profiler recorder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ProfilerRecorderConfiguration extends Configuration {
    private final String fileName;
    private final long delayPeriod;
    private final long recordPeriod;

    public ProfilerRecorderConfiguration(String fileName, long delayPeriod, long recordPeriod) {
        Assert.notNull(fileName);

        this.fileName = fileName;
        this.delayPeriod = delayPeriod;
        this.recordPeriod = recordPeriod;
    }

    public String getFileName() {
        return fileName;
    }

    public long getDelayPeriod() {
        return delayPeriod;
    }

    public long getRecordPeriod() {
        return recordPeriod;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ProfilerRecorderConfiguration))
            return false;

        ProfilerRecorderConfiguration configuration = (ProfilerRecorderConfiguration) o;
        return fileName.equals(configuration.fileName) && delayPeriod == configuration.delayPeriod &&
                recordPeriod == configuration.recordPeriod;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fileName, delayPeriod, recordPeriod);
    }

    @Override
    public String toString() {
        return fileName.toString();
    }
}
