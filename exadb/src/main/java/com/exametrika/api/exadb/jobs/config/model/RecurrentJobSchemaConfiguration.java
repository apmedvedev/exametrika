/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.util.Map;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.jobs.config.model.JobOperationSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.SchedulePeriodSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link RecurrentJobSchemaConfiguration} represents a configuration of recurrent job.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class RecurrentJobSchemaConfiguration extends JobSchemaConfiguration {
    private final long repeatCount;
    private final SchedulePeriodSchemaConfiguration period;

    public RecurrentJobSchemaConfiguration(String name, String description, String group, Map<String, Object> parameters,
                                           JobOperationSchemaConfiguration operation, ScheduleSchemaConfiguration schedule, boolean enabled,
                                           int restartCount, long restartPeriod, long maxExecutionPeriod, long repeatCount, SchedulePeriodSchemaConfiguration period) {
        super(name, description, group, parameters, operation, schedule, enabled, restartCount, restartPeriod, maxExecutionPeriod);

        Assert.notNull(period);

        this.repeatCount = repeatCount;
        this.period = period;
    }

    public long getRepeatCount() {
        return repeatCount;
    }

    public SchedulePeriodSchemaConfiguration getPeriod() {
        return period;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RecurrentJobSchemaConfiguration))
            return false;

        RecurrentJobSchemaConfiguration configuration = (RecurrentJobSchemaConfiguration) o;
        return super.equals(o) && repeatCount == configuration.repeatCount && period.equals(configuration.period);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(repeatCount, period);
    }
}