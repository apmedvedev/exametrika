/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.api.aggregator.common.values.config.NameValueSchemaConfiguration;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link CounterSchemaConfiguration} is a counter schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CounterSchemaConfiguration extends MetricTypeSchemaConfiguration {
    public CounterSchemaConfiguration(String name, List<? extends FieldValueSchemaConfiguration> fields,
                                      List<NameRepresentationSchemaConfiguration> representations) {
        super(name, new NameValueSchemaConfiguration(name, fields), representations);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CounterSchemaConfiguration))
            return false;

        CounterSchemaConfiguration configuration = (CounterSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
