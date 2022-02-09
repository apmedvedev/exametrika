/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.api.aggregator.common.values.config.StackValueSchemaConfiguration;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link StackCounterSchemaConfiguration} is a stack counter schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackCounterSchemaConfiguration extends MetricTypeSchemaConfiguration {
    public StackCounterSchemaConfiguration(String name, List<? extends FieldValueSchemaConfiguration> fields,
                                           List<StackRepresentationSchemaConfiguration> representations) {
        super(name, new StackValueSchemaConfiguration(name, fields), representations);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackCounterSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
