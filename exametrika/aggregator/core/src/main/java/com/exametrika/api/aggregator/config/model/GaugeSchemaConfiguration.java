/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.api.aggregator.common.values.config.NameValueSchemaConfiguration;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link GaugeSchemaConfiguration} is a gauge schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class GaugeSchemaConfiguration extends MetricTypeSchemaConfiguration {
    private final boolean sumByGroup;

    public GaugeSchemaConfiguration(String name, List<? extends FieldValueSchemaConfiguration> fields,
                                    List<NameRepresentationSchemaConfiguration> representations, boolean sumByGroup) {
        super(name, new NameValueSchemaConfiguration(name, fields), representations);

        this.sumByGroup = sumByGroup;
    }

    public boolean isSumByGroup() {
        return sumByGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof GaugeSchemaConfiguration))
            return false;

        GaugeSchemaConfiguration configuration = (GaugeSchemaConfiguration) o;
        return super.equals(o) && sumByGroup == configuration.sumByGroup;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(sumByGroup);
    }
}
