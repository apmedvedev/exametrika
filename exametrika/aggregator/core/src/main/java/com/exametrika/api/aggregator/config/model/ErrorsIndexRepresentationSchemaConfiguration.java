/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.impl.aggregator.values.ErrorsIndexAccessorFactory;
import com.exametrika.impl.aggregator.values.ErrorsIndexComputer;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.IMetricComputer;


/**
 * The {@link ErrorsIndexRepresentationSchemaConfiguration} is a configuration for errors index representation schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ErrorsIndexRepresentationSchemaConfiguration extends ObjectRepresentationSchemaConfiguration {
    public ErrorsIndexRepresentationSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public IMetricComputer createComputer(ComponentValueSchemaConfiguration componentSchema,
                                          ComponentRepresentationSchemaConfiguration componentConfiguration, IComponentAccessorFactory componentAccessorFactory,
                                          int metricIndex) {
        return new ErrorsIndexComputer(componentConfiguration, componentAccessorFactory);
    }

    @Override
    public IMetricAccessorFactory createAccessorFactory(ComponentValueSchemaConfiguration componentSchema,
                                                        ComponentRepresentationSchemaConfiguration componentConfiguration, int metricIndex) {
        return new ErrorsIndexAccessorFactory(componentConfiguration, metricIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ErrorsIndexRepresentationSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
