/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.util.Map;

import com.exametrika.spi.exadb.jobs.config.model.JobOperationSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link OneTimeJobSchemaConfiguration} represents a configuration of one-time job.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class OneTimeJobSchemaConfiguration extends JobSchemaConfiguration {
    public OneTimeJobSchemaConfiguration(String name, String description, String group, Map<String, Object> parameters,
                                         JobOperationSchemaConfiguration operation, ScheduleSchemaConfiguration schedule, boolean enabled,
                                         int restartCount, long restartPeriod, long maxExecutionPeriod) {
        super(name, description, group, parameters, operation, schedule, enabled, restartCount, restartPeriod, maxExecutionPeriod);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof OneTimeJobSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}