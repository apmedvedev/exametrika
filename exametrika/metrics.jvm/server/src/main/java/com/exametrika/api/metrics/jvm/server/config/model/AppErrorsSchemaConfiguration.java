/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.server.config.model;

import java.util.List;

import com.exametrika.api.aggregator.config.model.MetricTypeSchemaConfiguration;


/**
 * The {@link AppErrorsSchemaConfiguration} is a application errors schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AppErrorsSchemaConfiguration extends MetricTypeSchemaConfiguration {
    public AppErrorsSchemaConfiguration(String name, String baseRepresentation,
                                        List<AppErrorsRepresentationSchemaConfiguration> representations) {
        super(name, new AppErrorsValueSchemaConfiguration(name, baseRepresentation), representations);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AppErrorsSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
