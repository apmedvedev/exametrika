/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.impl.aggregator.values.HealthIndexComputer;
import com.exametrika.impl.aggregator.values.HealthIndexAccessorFactory;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.IMetricComputer;


/**
 * The {@link HealthIndexRepresentationSchemaConfiguration} is a configuration for health index representation schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HealthIndexRepresentationSchemaConfiguration extends ObjectRepresentationSchemaConfiguration {
    public HealthIndexRepresentationSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public IMetricComputer createComputer(ComponentValueSchemaConfiguration componentSchema,
                                          ComponentRepresentationSchemaConfiguration componentConfiguration, IComponentAccessorFactory componentAccessorFactory,
                                          int metricIndex) {
        return new HealthIndexComputer(componentSchema.getMetrics().get(metricIndex).getName(), componentConfiguration, componentAccessorFactory);
    }

    @Override
    public IMetricAccessorFactory createAccessorFactory(ComponentValueSchemaConfiguration componentSchema,
                                                        ComponentRepresentationSchemaConfiguration componentConfiguration, int metricIndex) {
        return new HealthIndexAccessorFactory(componentSchema.getMetrics().get(metricIndex).getName(), componentConfiguration, metricIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HealthIndexRepresentationSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
