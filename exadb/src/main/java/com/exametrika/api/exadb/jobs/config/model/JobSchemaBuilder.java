/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.Kind;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.UnitType;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.config.model.JobOperationSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.SchedulePeriodSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link JobSchemaBuilder} represents a builder of job configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class JobSchemaBuilder {
    private String name;
    private String description;
    private String group;
    private Map<String, Object> parameters;
    private JobOperationSchemaConfiguration operation;
    private ScheduleSchemaConfiguration schedule;
    private boolean enabled;
    private int restartCount;
    private long restartPeriod;
    private long repeatCount;
    private long maxExecutionPeriod;
    private SchedulePeriodSchemaConfiguration period;
    private boolean recurrent;

    public class ParametersBuilder {
        public ParametersBuilder put(String key, Object value) {
            parameters.put(key, value);
            return this;
        }

        public ParametersBuilder remove(String key) {
            parameters.remove(key);
            return this;
        }

        public ParametersBuilder clear() {
            parameters.clear();
            return this;
        }

        public JobSchemaBuilder end() {
            return JobSchemaBuilder.this;
        }
    }

    public JobSchemaBuilder() {
        this.description = "";
        this.group = null;
        this.parameters = new LinkedHashMap<String, Object>();
        this.schedule = Schedules.time().from(0).to(23).toSchedule();
        this.enabled = true;
        this.restartCount = 3;
        this.restartPeriod = 10000;
        this.repeatCount = Long.MAX_VALUE;
        this.period = new StandardSchedulePeriodSchemaConfiguration(UnitType.SECOND, Kind.RELATIVE, 1, null);
    }

    public JobSchemaBuilder(JobSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.name = configuration.getName();
        this.description = configuration.getDescription();
        this.group = configuration.getGroup();
        this.parameters = new LinkedHashMap<String, Object>(configuration.getParameters());
        this.schedule = configuration.getSchedule();
        this.enabled = configuration.isEnabled();
        this.restartCount = configuration.getRestartCount();
        this.restartPeriod = configuration.getRestartPeriod();

        if (configuration instanceof RecurrentJobSchemaConfiguration) {
            repeatCount = ((RecurrentJobSchemaConfiguration) configuration).getRepeatCount();
            period = ((RecurrentJobSchemaConfiguration) configuration).getPeriod();
            recurrent = true;
        }
    }

    public JobSchemaBuilder name(String value) {
        Assert.notNull(value);

        this.name = value;
        return this;
    }

    public JobSchemaBuilder description(String value) {
        Assert.notNull(value);

        this.description = value;
        return this;
    }

    public JobSchemaBuilder group(String value) {
        this.group = value;
        return this;
    }

    public ParametersBuilder parameters() {
        return new ParametersBuilder();
    }

    public JobSchemaBuilder operation(JobOperationSchemaConfiguration value) {
        Assert.notNull(value);

        this.operation = value;
        return this;
    }

    public JobSchemaBuilder schedule(ScheduleSchemaConfiguration value) {
        Assert.notNull(value);

        this.schedule = value;
        return this;
    }

    public JobSchemaBuilder enabled(boolean value) {
        this.enabled = value;
        return this;
    }

    public JobSchemaBuilder restartCount(int value) {
        this.restartCount = value;
        return this;
    }

    public JobSchemaBuilder restartPeriod(long value) {
        this.restartPeriod = value;
        return this;
    }

    public JobSchemaBuilder maxExecutionPeriod(long value) {
        this.maxExecutionPeriod = value;
        return this;
    }

    public JobSchemaBuilder repeatCount(long value) {
        Assert.checkState(recurrent);

        this.repeatCount = value;
        return this;
    }

    public JobSchemaBuilder period(SchedulePeriodSchemaConfiguration value) {
        Assert.notNull(period);
        Assert.checkState(recurrent);

        this.period = value;
        return this;
    }

    public JobSchemaBuilder recurrent() {
        this.recurrent = true;
        return this;
    }

    public JobSchemaBuilder oneTime() {
        this.recurrent = false;
        return this;
    }

    public JobSchemaConfiguration toJob() {
        if (recurrent)
            return new RecurrentJobSchemaConfiguration(name, description, group, new LinkedHashMap<String, Object>(parameters), operation, schedule, enabled, restartCount,
                    restartPeriod, maxExecutionPeriod, repeatCount, period);
        else
            return new OneTimeJobSchemaConfiguration(name, description, group, new LinkedHashMap<String, Object>(parameters), operation, schedule, enabled, restartCount,
                    restartPeriod, maxExecutionPeriod);
    }
}