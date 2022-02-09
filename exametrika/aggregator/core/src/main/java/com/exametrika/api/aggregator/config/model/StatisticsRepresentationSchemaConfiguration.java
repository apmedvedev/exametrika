/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.values.StatisticsAccessor;
import com.exametrika.impl.aggregator.values.StatisticsComputer;
import com.exametrika.impl.aggregator.values.StatisticsAccessor.Type;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;


/**
 * The {@link StatisticsRepresentationSchemaConfiguration} is a statistics aggergation field schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StatisticsRepresentationSchemaConfiguration extends FieldRepresentationSchemaConfiguration {
    public StatisticsRepresentationSchemaConfiguration(boolean enabled) {
        super("stat", enabled);
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
        return new StatisticsAccessor(getType(fieldName), (StatisticsComputer) createComputer(schema, accessorFactory));
    }

    @Override
    public IFieldComputer createComputer(FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        return new StatisticsComputer(accessorFactory.createAccessor(null, null, "std.count"),
                accessorFactory.createAccessor(null, null, "std.avg"));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StatisticsRepresentationSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    private Type getType(String fieldName) {
        if (fieldName.equals("stddev"))
            return Type.STANDARD_DEVIATION;
        else if (fieldName.equals("vc"))
            return Type.VARIATION_COEFFICIENT;
        else {
            Assert.isTrue(false);
            return null;
        }
    }
}
