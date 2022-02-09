/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.api.aggregator.common.values.config.ObjectValueSchemaConfiguration;


/**
 * The {@link ComputedMetricSchemaConfiguration} is a computed metric schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComputedMetricSchemaConfiguration extends MetricTypeSchemaConfiguration {
    public ComputedMetricSchemaConfiguration(String name, List<MetricRepresentationSchemaConfiguration> representations) {
        super(name, new ObjectValueSchemaConfiguration(name), representations);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ComputedMetricSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
