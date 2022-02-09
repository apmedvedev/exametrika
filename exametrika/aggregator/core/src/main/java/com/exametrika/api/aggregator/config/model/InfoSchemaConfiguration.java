/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.api.aggregator.common.values.config.ObjectValueSchemaConfiguration;


/**
 * The {@link InfoSchemaConfiguration} is a info schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InfoSchemaConfiguration extends MetricTypeSchemaConfiguration {
    public InfoSchemaConfiguration(String name, List<ObjectRepresentationSchemaConfiguration> representations) {
        super(name, new ObjectValueSchemaConfiguration(name), representations);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof InfoSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
