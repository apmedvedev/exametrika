/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import java.util.List;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;


/**
 * The {@link MetricHierarchy} is a hierarchy of metrics from parents to children.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MetricHierarchy {
    private final List<IMetricName> metrics;

    public MetricHierarchy(List<? extends IMetricName> metrics) {
        Assert.notNull(metrics);

        this.metrics = Immutables.wrap(metrics);
    }

    public List<IMetricName> getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return metrics.toString();
    }
}
