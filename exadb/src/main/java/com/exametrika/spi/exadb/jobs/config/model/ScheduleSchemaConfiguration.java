/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.jobs.config.model;

import java.util.Locale;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.exadb.jobs.ISchedule;

/**
 * The {@link ScheduleSchemaConfiguration} represents a configuration of schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ScheduleSchemaConfiguration extends Configuration {
    public abstract ISchedule createSchedule();

    public abstract String toString(Locale locale);
}