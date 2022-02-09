/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.server.config.model;

import java.util.List;

import com.exametrika.api.aggregator.config.model.MetricTypeSchemaConfiguration;


/**
 * The {@link JvmErrorsSchemaConfiguration} is a jvm errors schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JvmErrorsSchemaConfiguration extends MetricTypeSchemaConfiguration {
    public static final String SCHEMA = "com.exametrika.metrics.jvm.server-1.0";

    public JvmErrorsSchemaConfiguration(String name, String baseRepresentation,
                                        List<JvmErrorsRepresentationSchemaConfiguration> representations) {
        super(name, new JvmErrorsValueSchemaConfiguration(name, baseRepresentation), representations);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JvmErrorsSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
