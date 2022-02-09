/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;


/**
 * The {@link ObjectValueSchemaBuilder} is a builder for aggregation metric value schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ObjectValueSchemaBuilder extends MetricValueSchemaBuilder {
    public ObjectValueSchemaBuilder(ComponentValueSchemaBuilder parent, String name) {
        super(parent, name);
    }

    @Override
    public MetricValueSchemaConfiguration toConfiguration() {
        return new ObjectValueSchemaConfiguration(name);
    }
}
