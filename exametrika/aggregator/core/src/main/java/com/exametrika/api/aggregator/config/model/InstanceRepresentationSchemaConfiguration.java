/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.values.InstanceAccessor;
import com.exametrika.impl.aggregator.values.InstanceComputer;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;


/**
 * The {@link InstanceRepresentationSchemaConfiguration} is a configuration of instance aggregation field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstanceRepresentationSchemaConfiguration extends FieldRepresentationSchemaConfiguration {
    public InstanceRepresentationSchemaConfiguration(boolean enabled) {
        super("instance", enabled);
    }

    @Override
    public boolean isValueSupported() {
        return true;
    }

    @Override
    public boolean isSecondaryComputationSupported() {
        return false;
    }

    @Override
    public IFieldAccessor createAccessor(String fieldName, FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        Assert.isTrue(fieldName.isEmpty());
        return new InstanceAccessor();
    }

    @Override
    public IFieldComputer createComputer(FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        return new InstanceComputer();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof InstanceRepresentationSchemaConfiguration))
            return false;

        InstanceRepresentationSchemaConfiguration configuration = (InstanceRepresentationSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
