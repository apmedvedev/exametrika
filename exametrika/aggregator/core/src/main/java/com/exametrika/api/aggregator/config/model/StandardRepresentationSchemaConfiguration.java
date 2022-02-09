/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.values.StandardAccessor;
import com.exametrika.impl.aggregator.values.StandardComputer;
import com.exametrika.impl.aggregator.values.StandardAccessor.Type;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;


/**
 * The {@link StandardRepresentationSchemaConfiguration} is a standard aggregation field schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StandardRepresentationSchemaConfiguration extends FieldRepresentationSchemaConfiguration {
    public StandardRepresentationSchemaConfiguration(boolean enabled) {
        super("std", enabled);
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
        return new StandardAccessor(getType(fieldName));
    }

    @Override
    public IFieldComputer createComputer(FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        return new StandardComputer();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StandardRepresentationSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    private Type getType(String fieldName) {
        if (fieldName.equals("count"))
            return Type.COUNT;
        else if (fieldName.equals("sum"))
            return Type.SUM;
        else if (fieldName.equals("min"))
            return Type.MIN;
        else if (fieldName.equals("max"))
            return Type.MAX;
        else if (fieldName.equals("avg"))
            return Type.AVERAGE;
        else {
            Assert.isTrue(false);
            return null;
        }
    }
}
