/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.host.server.config.model;

import java.util.List;

import com.exametrika.api.aggregator.config.model.MetricTypeSchemaConfiguration;


/**
 * The {@link HostWorkloadSchemaConfiguration} is a host workload schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostWorkloadSchemaConfiguration extends MetricTypeSchemaConfiguration {
    public HostWorkloadSchemaConfiguration(String name, String baseRepresentation,
                                           List<HostWorkloadRepresentationSchemaConfiguration> representations) {
        super(name, new HostWorkloadValueSchemaConfiguration(name, baseRepresentation), representations);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HostWorkloadSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
