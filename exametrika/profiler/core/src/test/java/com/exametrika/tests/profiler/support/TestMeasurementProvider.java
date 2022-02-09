/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;

import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;

public class TestMeasurementProvider implements IMeasurementProvider {
    public Object value;

    @Override
    public Object getValue() throws Exception {
        return value;
    }
}