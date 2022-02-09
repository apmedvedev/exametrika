/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.server.config.model;

import java.util.List;

import com.exametrika.api.aggregator.config.model.MetricTypeSchemaConfiguration;


/**
 * The {@link AppWorkloadSchemaConfiguration} is a application workload schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AppWorkloadSchemaConfiguration extends MetricTypeSchemaConfiguration {
    public AppWorkloadSchemaConfiguration(String name, String baseRepresentation,
                                          List<AppWorkloadRepresentationSchemaConfiguration> representations) {
        super(name, new AppWorkloadValueSchemaConfiguration(name, baseRepresentation), representations);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AppWorkloadSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
