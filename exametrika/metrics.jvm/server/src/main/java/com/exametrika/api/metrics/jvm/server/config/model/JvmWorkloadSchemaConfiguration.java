/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.server.config.model;

import java.util.List;

import com.exametrika.api.aggregator.config.model.MetricTypeSchemaConfiguration;


/**
 * The {@link JvmWorkloadSchemaConfiguration} is a jvm workload schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JvmWorkloadSchemaConfiguration extends MetricTypeSchemaConfiguration {
    public JvmWorkloadSchemaConfiguration(String name, String baseRepresentation,
                                          List<JvmWorkloadRepresentationSchemaConfiguration> representations) {
        super(name, new JvmWorkloadValueSchemaConfiguration(name, baseRepresentation), representations);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JvmWorkloadSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
