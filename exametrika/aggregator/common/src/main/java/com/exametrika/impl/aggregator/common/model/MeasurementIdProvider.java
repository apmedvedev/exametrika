/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.model;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.api.aggregator.common.model.IMeasurementIdProvider;
import com.exametrika.common.utils.Assert;


/**
 * The {@link MeasurementIdProvider} represents an implementation of {@link IMeasurementIdProvider}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MeasurementIdProvider implements IMeasurementIdProvider {
    private final IMeasurementId id;

    public MeasurementIdProvider(IMeasurementId id) {
        Assert.notNull(id);

        this.id = id;
    }

    @Override
    public IMeasurementId get() {
        return id;
    }
}
