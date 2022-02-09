/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ObjectValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link AnomalyIndexValueSchemaConfiguration} is a object value schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AnomalyIndexValueSchemaConfiguration extends ObjectValueSchemaConfiguration {
    private final String baseRepresentation;
    private final int minAnomalyMetricCount;

    public AnomalyIndexValueSchemaConfiguration(String name, String baseRepresentation, int minAnomalyMetricCount) {
        super(name);

        Assert.notNull(baseRepresentation);

        this.baseRepresentation = baseRepresentation;
        this.minAnomalyMetricCount = minAnomalyMetricCount;
    }

    public String getBaseRepresentation() {
        return baseRepresentation;
    }

    public int getMinAnomalyMetricCount() {
        return minAnomalyMetricCount;
    }

    @Override
    public void buildBaseRepresentations(Set<String> baseRepresentations) {
        baseRepresentations.add(baseRepresentation);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AnomalyIndexValueSchemaConfiguration))
            return false;

        AnomalyIndexValueSchemaConfiguration configuration = (AnomalyIndexValueSchemaConfiguration) o;
        return super.equals(o) && baseRepresentation.equals(configuration.baseRepresentation) &&
                minAnomalyMetricCount == configuration.minAnomalyMetricCount;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(baseRepresentation, minAnomalyMetricCount);
    }
}
