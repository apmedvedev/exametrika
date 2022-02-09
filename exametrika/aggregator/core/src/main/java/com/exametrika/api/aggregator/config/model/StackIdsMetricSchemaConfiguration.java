/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.Collections;

import com.exametrika.api.aggregator.common.values.config.StackIdsValueSchemaConfiguration;


/**
 * The {@link StackIdsMetricSchemaConfiguration} is a stackIds metric schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackIdsMetricSchemaConfiguration extends MetricTypeSchemaConfiguration {
    public StackIdsMetricSchemaConfiguration(String name) {
        super(name, new StackIdsValueSchemaConfiguration(name), Collections.<MetricRepresentationSchemaConfiguration>emptyList());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackIdsMetricSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
