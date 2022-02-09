/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.host.server.config.model;

import java.util.List;

import com.exametrika.api.aggregator.config.model.MetricTypeSchemaConfiguration;


/**
 * The {@link HostErrorsSchemaConfiguration} is a host errors schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostErrorsSchemaConfiguration extends MetricTypeSchemaConfiguration {
    public static final String SCHEMA = "com.exametrika.metrics.host.server-1.0";

    public HostErrorsSchemaConfiguration(String name, String baseRepresentation,
                                         List<HostErrorsRepresentationSchemaConfiguration> representations) {
        super(name, new HostErrorsValueSchemaConfiguration(name, baseRepresentation), representations);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HostErrorsSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
