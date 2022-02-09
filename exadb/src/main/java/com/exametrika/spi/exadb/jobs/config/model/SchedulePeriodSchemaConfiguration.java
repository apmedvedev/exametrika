/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.jobs.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.exadb.jobs.ISchedulePeriod;

/**
 * The {@link SchedulePeriodSchemaConfiguration} represents a configuration of schedule period.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class SchedulePeriodSchemaConfiguration extends Configuration {
    public abstract ISchedulePeriod createPeriod();
}