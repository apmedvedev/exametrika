/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.values.PeriodAccessor;
import com.exametrika.impl.aggregator.values.PeriodComputer;
import com.exametrika.impl.aggregator.values.PeriodAccessor.Type;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;


/**
 * The {@link PeriodRepresentationSchemaConfiguration} is a period aggregation field schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodRepresentationSchemaConfiguration extends FieldRepresentationSchemaConfiguration {
    public static final String PERIOD_ACCESSOR = "period";
    private final String baseField;
    private final String navigationType;

    public PeriodRepresentationSchemaConfiguration(String baseField, boolean enabled) {
        this(null, PERIOD_ACCESSOR, baseField, enabled);
    }

    public PeriodRepresentationSchemaConfiguration(String name, String navigationType, String baseField, boolean enabled) {
        super(name != null ? name : ("period(" + baseField + ")"), enabled);

        Assert.notNull(navigationType);
        Assert.notNull(baseField);

        this.navigationType = navigationType;
        this.baseField = baseField;
    }

    public String getNavigationType() {
        return navigationType;
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
        return new PeriodAccessor(getType(fieldName), (PeriodComputer) createComputer(schema, accessorFactory));
    }

    @Override
    public IFieldComputer createComputer(FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        return new PeriodComputer(accessorFactory.createAccessor(null, null, baseField), accessorFactory.createAccessor(navigationType, null, baseField));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PeriodRepresentationSchemaConfiguration))
            return false;

        PeriodRepresentationSchemaConfiguration configuration = (PeriodRepresentationSchemaConfiguration) o;
        return super.equals(o) && navigationType.equals(configuration.navigationType) && baseField.equals(configuration.baseField);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(navigationType, baseField);
    }

    private Type getType(String fieldName) {
        if (fieldName.equals("delta"))
            return Type.DELTA;
        else if (fieldName.equals("delta%"))
            return Type.DELTA_PERCENTAGE;
        else
            return Assert.error();
    }
}
