/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.IMetricComputer;


/**
 * The {@link MetricRepresentationSchemaConfiguration} is a aggregation metric value representation schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class MetricRepresentationSchemaConfiguration extends Configuration {
    private final String name;

    public MetricRepresentationSchemaConfiguration(String name) {
        Assert.notNull(name);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract IMetricComputer createComputer(ComponentValueSchemaConfiguration componentSchema,
                                                   ComponentRepresentationSchemaConfiguration componentConfiguration, IComponentAccessorFactory componentAccessorFactory,
                                                   int metricIndex);

    public abstract IMetricAccessorFactory createAccessorFactory(ComponentValueSchemaConfiguration componentSchema,
                                                                 ComponentRepresentationSchemaConfiguration componentConfiguration, int metricIndex);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MetricRepresentationSchemaConfiguration))
            return false;

        MetricRepresentationSchemaConfiguration configuration = (MetricRepresentationSchemaConfiguration) o;
        return name.equals(configuration.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
