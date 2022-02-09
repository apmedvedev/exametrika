/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.values.RateAccessor;
import com.exametrika.impl.aggregator.values.RateComputer;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;


/**
 * The {@link RateRepresentationSchemaConfiguration} is a rate aggregation field schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class RateRepresentationSchemaConfiguration extends FieldRepresentationSchemaConfiguration {
    private final String baseField;

    public RateRepresentationSchemaConfiguration(String baseField, boolean enabled) {
        this(null, baseField, enabled);
    }

    public RateRepresentationSchemaConfiguration(String name, String baseField, boolean enabled) {
        super(name != null ? name : ("rate(" + baseField + ")"), enabled);

        Assert.notNull(baseField);

        this.baseField = baseField;
    }

    public String getBaseField() {
        return baseField;
    }

    @Override
    public boolean isValueSupported() {
        return false;
    }

    @Override
    public boolean isSecondaryComputationSupported() {
        return false;
    }

    @Override
    public IFieldAccessor createAccessor(String fieldName, FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        Assert.isTrue(fieldName.isEmpty());
        return new RateAccessor((RateComputer) createComputer(schema, accessorFactory));
    }

    @Override
    public IFieldComputer createComputer(FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        return new RateComputer(accessorFactory.createAccessor(null, null, baseField));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RateRepresentationSchemaConfiguration))
            return false;

        RateRepresentationSchemaConfiguration configuration = (RateRepresentationSchemaConfiguration) o;
        return super.equals(o) && baseField.equals(configuration.baseField);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + baseField.hashCode();
    }
}
