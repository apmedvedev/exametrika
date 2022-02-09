/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.api.aggregator.common.values.config.ObjectValueSchemaConfiguration;


/**
 * The {@link ExpressionIndexSchemaConfiguration} is a expression index schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionIndexSchemaConfiguration extends MetricTypeSchemaConfiguration {
    public ExpressionIndexSchemaConfiguration(String name, boolean stored, String baseRepresentation,
                                              List<ExpressionIndexRepresentationSchemaConfiguration> representations) {
        super(name, stored ? new ExpressionIndexValueSchemaConfiguration(name, baseRepresentation) : new ObjectValueSchemaConfiguration(name),
                representations);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExpressionIndexSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
