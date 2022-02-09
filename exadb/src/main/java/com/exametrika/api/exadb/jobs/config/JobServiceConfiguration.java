/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config;

import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.DomainServiceConfiguration;

/**
 * The {@link JobServiceConfiguration} represents a configuration of job service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobServiceConfiguration extends DomainServiceConfiguration {
    public static final String SCHEMA = "com.exametrika.exadb.jobs-1.0";
    public static final String NAME = "system.JobService";

    private final int threadCount;
    private final long schedulePeriod;

    public JobServiceConfiguration() {
        this(Runtime.getRuntime().availableProcessors() * 2, 100);
    }

    public JobServiceConfiguration(int threadCount, long schedulePeriod) {
        super(NAME);

        this.threadCount = threadCount;
        this.schedulePeriod = schedulePeriod;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public long getSchedulePeriod() {
        return schedulePeriod;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JobServiceConfiguration))
            return false;

        JobServiceConfiguration configuration = (JobServiceConfiguration) o;
        return threadCount == configuration.threadCount && schedulePeriod == configuration.schedulePeriod;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(threadCount, schedulePeriod);
    }
}