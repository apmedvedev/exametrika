/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.util.Map;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.jobs.config.model.JobOperationSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link JobSchemaConfiguration} represents a configuration of job.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class JobSchemaConfiguration extends Configuration {
    private final String name;
    private final String description;
    private final String group;
    private final Map<String, Object> parameters;
    private final JobOperationSchemaConfiguration operation;
    private final ScheduleSchemaConfiguration schedule;
    private final boolean enabled;
    private final int restartCount;
    private final long restartPeriod;
    private final long maxExecutionPeriod;

    public JobSchemaConfiguration(String name, String description, String group, Map<String, Object> parameters, JobOperationSchemaConfiguration operation,
                                  ScheduleSchemaConfiguration schedule, boolean enabled, int restartCount, long restartPeriod, long maxExecutionPeriod) {
        Assert.notNull(name);
        Assert.notNull(schedule);
        Assert.notNull(operation);

        this.name = name;
        this.description = description != null ? description : "";
        this.group = group;
        this.parameters = parameters;
        this.operation = operation;
        this.schedule = schedule;
        this.enabled = enabled;
        this.restartCount = restartCount;
        this.restartPeriod = restartPeriod;
        this.maxExecutionPeriod = maxExecutionPeriod;
    }

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public final String getGroup() {
        return group;
    }

    public final Map<String, Object> getParameters() {
        return parameters;
    }

    public final JobOperationSchemaConfiguration getOperation() {
        return operation;
    }

    public final ScheduleSchemaConfiguration getSchedule() {
        return schedule;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final int getRestartCount() {
        return restartCount;
    }

    public final long getRestartPeriod() {
        return restartPeriod;
    }

    public final long getMaxExecutionPeriod() {
        return maxExecutionPeriod;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JobSchemaConfiguration))
            return false;

        JobSchemaConfiguration configuration = (JobSchemaConfiguration) o;
        return name.equals(configuration.name) && description.equals(configuration.description) &&
                Objects.equals(group, configuration.group) && Objects.equals(parameters, configuration.parameters) &&
                operation.equals(configuration.operation) && schedule.equals(configuration.schedule) &&
                enabled == configuration.enabled && restartCount == configuration.restartCount &&
                restartPeriod == configuration.restartPeriod && maxExecutionPeriod == configuration.maxExecutionPeriod;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, description, group, parameters, operation, schedule, enabled, restartCount, restartPeriod,
                maxExecutionPeriod);
    }

    @Override
    public String toString() {
        return name;
    }
}