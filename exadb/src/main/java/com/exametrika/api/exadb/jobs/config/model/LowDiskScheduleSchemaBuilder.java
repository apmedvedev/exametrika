/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaBuilder;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;


/**
 * The {@link LowDiskScheduleSchemaBuilder} represents a low disk schedule builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class LowDiskScheduleSchemaBuilder extends ScheduleSchemaBuilder {
    private final CompositeScheduleSchemaBuilder parent;
    private String path;
    private long minFreeSpace;
    private boolean included = true;

    public LowDiskScheduleSchemaBuilder() {
        parent = null;
    }

    public LowDiskScheduleSchemaBuilder(CompositeScheduleSchemaBuilder parent) {
        Assert.notNull(parent);

        this.parent = parent;
    }

    public LowDiskScheduleSchemaBuilder minFreeSpace(long minFreeSpace) {
        this.minFreeSpace = minFreeSpace;
        return this;
    }

    public LowDiskScheduleSchemaBuilder path(String path) {
        Assert.notNull(path);

        this.path = path;
        return this;
    }

    public LowDiskScheduleSchemaBuilder exclude() {
        included = false;
        return this;
    }

    public CompositeScheduleSchemaBuilder end() {
        Assert.checkState(parent != null);
        return parent;
    }

    @Override
    public ScheduleSchemaConfiguration toSchedule() {
        return new LowDiskScheduleSchemaConfiguration(path, minFreeSpace, included);
    }
}