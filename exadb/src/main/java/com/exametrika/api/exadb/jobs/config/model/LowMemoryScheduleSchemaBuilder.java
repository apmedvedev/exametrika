/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaBuilder;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;


/**
 * The {@link LowMemoryScheduleSchemaBuilder} represents a low memory schedule builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class LowMemoryScheduleSchemaBuilder extends ScheduleSchemaBuilder {
    private final CompositeScheduleSchemaBuilder parent;
    private long minFreeSpace;
    private boolean included = true;

    public LowMemoryScheduleSchemaBuilder() {
        parent = null;
    }

    public LowMemoryScheduleSchemaBuilder(CompositeScheduleSchemaBuilder parent) {
        Assert.notNull(parent);

        this.parent = parent;
    }

    public LowMemoryScheduleSchemaBuilder minFreeSpace(long minFreeSpace) {
        this.minFreeSpace = minFreeSpace;
        return this;
    }

    public LowMemoryScheduleSchemaBuilder exclude() {
        included = false;
        return this;
    }

    public CompositeScheduleSchemaBuilder end() {
        Assert.checkState(parent != null);
        return parent;
    }

    @Override
    public ScheduleSchemaConfiguration toSchedule() {
        return new LowMemoryScheduleSchemaConfiguration(minFreeSpace, included);
    }
}