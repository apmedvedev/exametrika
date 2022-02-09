/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import com.exametrika.common.utils.Assert;


/**
 * The {@link MetricValueSchemaBuilder} is a builder for aggregation metric value schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class MetricValueSchemaBuilder {
    private final ComponentValueSchemaBuilder parent;
    protected final String name;

    public MetricValueSchemaBuilder(ComponentValueSchemaBuilder parent, String name) {
        Assert.notNull(parent);
        Assert.notNull(name);

        this.parent = parent;
        this.name = name;
    }

    public ComponentValueSchemaBuilder end() {
        return parent;
    }

    public abstract MetricValueSchemaConfiguration toConfiguration();
}
