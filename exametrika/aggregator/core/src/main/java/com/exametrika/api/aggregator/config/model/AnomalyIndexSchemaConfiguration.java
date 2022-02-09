/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;


/**
 * The {@link AnomalyIndexSchemaConfiguration} is a anomaly index schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AnomalyIndexSchemaConfiguration extends MetricTypeSchemaConfiguration {
    public AnomalyIndexSchemaConfiguration(String name, String baseRepresentation, int minAnomalyMetricCount,
                                           List<AnomalyIndexRepresentationSchemaConfiguration> representations) {
        super(name, new AnomalyIndexValueSchemaConfiguration(name, baseRepresentation, minAnomalyMetricCount), representations);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AnomalyIndexSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
