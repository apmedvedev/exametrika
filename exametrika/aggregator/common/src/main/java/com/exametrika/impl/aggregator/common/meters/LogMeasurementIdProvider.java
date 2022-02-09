/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.meters;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.api.aggregator.common.model.IMeasurementIdProvider;
import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;

/**
 * The {@link LogMeasurementIdProvider} is a log measurement id provider.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class LogMeasurementIdProvider implements IMeasurementIdProvider {
    private final IMeasurementIdProvider provider;
    private final String metricType;

    public LogMeasurementIdProvider(IMeasurementIdProvider provider, String metricType) {
        this.provider = provider;
        this.metricType = metricType;
    }

    @Override
    public IMeasurementId get() {
        IMeasurementId id = provider.get();
        if (id instanceof NameMeasurementId)
            return new NameMeasurementId(((NameMeasurementId) id).getScope(), ((NameMeasurementId) id).getLocation(), metricType);
        else
            return new MeasurementId(((MeasurementId) id).getScopeId(), ((MeasurementId) id).getLocationId(), metricType);
    }
}