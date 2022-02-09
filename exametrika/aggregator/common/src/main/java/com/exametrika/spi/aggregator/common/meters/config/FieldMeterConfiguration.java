/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.api.aggregator.common.meters.config.StandardFieldConfiguration;
import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.NameValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link FieldMeterConfiguration} is a configuration of meter that supports fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class FieldMeterConfiguration extends MeterConfiguration {
    private final List<FieldConfiguration> fields;

    public FieldMeterConfiguration(boolean enabled) {
        this(enabled, Collections.singletonList(new StandardFieldConfiguration()));
    }

    public FieldMeterConfiguration(boolean enabled, List<? extends FieldConfiguration> fields) {
        super(enabled);

        Assert.notNull(fields);
        Assert.isTrue(!fields.isEmpty());

        this.fields = Immutables.wrap(fields);
    }

    public final List<FieldConfiguration> getFields() {
        return fields;
    }

    @Override
    public MetricValueSchemaConfiguration getSchema(String metricType) {
        List<FieldValueSchemaConfiguration> fields = new ArrayList<FieldValueSchemaConfiguration>(this.fields.size());
        for (FieldConfiguration field : this.fields)
            fields.add(field.getSchema());
        return new NameValueSchemaConfiguration(metricType, fields);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FieldMeterConfiguration))
            return false;

        FieldMeterConfiguration configuration = (FieldMeterConfiguration) o;
        return super.equals(configuration) && fields.equals(configuration.fields);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(fields);
    }
}
