/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.jobs.config.model;


/**
 * The {@link ScheduleSchemaBuilder} represents an abstract schedule builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class ScheduleSchemaBuilder {
    public abstract ScheduleSchemaConfiguration toSchedule();
}