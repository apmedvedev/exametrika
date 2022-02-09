/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.impl.aggregator.values.AnomalyIndexAccessorFactory;
import com.exametrika.impl.aggregator.values.AnomalyIndexComputer;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.IMetricComputer;


/**
 * The {@link AnomalyIndexRepresentationSchemaConfiguration} is a anomaly index representation schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AnomalyIndexRepresentationSchemaConfiguration extends ObjectRepresentationSchemaConfiguration {
    public AnomalyIndexRepresentationSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public IMetricComputer createComputer(ComponentValueSchemaConfiguration componentSchema,
                                          ComponentRepresentationSchemaConfiguration componentConfiguration, IComponentAccessorFactory componentAccessorFactory,
                                          int metricIndex) {
        return new AnomalyIndexComputer(componentSchema, metricIndex);
    }

    @Override
    public IMetricAccessorFactory createAccessorFactory(ComponentValueSchemaConfiguration componentSchema,
                                                        ComponentRepresentationSchemaConfiguration componentConfiguration, int metricIndex) {
        return new AnomalyIndexAccessorFactory(componentSchema, metricIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AnomalyIndexRepresentationSchemaConfiguration))
            return false;

        AnomalyIndexRepresentationSchemaConfiguration configuration = (AnomalyIndexRepresentationSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
