/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link FieldRepresentationSchemaConfiguration} is a aggregation field representation schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class FieldRepresentationSchemaConfiguration extends Configuration {
    private final String name;
    private final boolean enabled;

    public FieldRepresentationSchemaConfiguration(String name, boolean enabled) {
        Assert.notNull(name);

        this.name = name;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public abstract boolean isValueSupported();

    public abstract boolean isSecondaryComputationSupported();

    public abstract IFieldAccessor createAccessor(String fieldName, FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory);

    public abstract IFieldComputer createComputer(FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FieldRepresentationSchemaConfiguration))
            return false;

        FieldRepresentationSchemaConfiguration configuration = (FieldRepresentationSchemaConfiguration) o;
        return name.equals(configuration.name) && enabled == configuration.enabled;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, enabled);
    }

    @Override
    public String toString() {
        return name;
    }
}
