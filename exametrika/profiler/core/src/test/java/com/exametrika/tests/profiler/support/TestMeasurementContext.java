/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;

import com.exametrika.spi.aggregator.common.meters.IMeasurementContext;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;

public class TestMeasurementContext implements IMeasurementContext {
    public IMeasurementHandler measurementHandler;

    public TestMeasurementContext(IMeasurementHandler measurementHandler) {
        this.measurementHandler = measurementHandler;
    }

    @Override
    public int getSchemaVersion() {
        return 1;
    }

    @Override
    public IMeasurementHandler getMeasurementHandler() {
        return measurementHandler;
    }
}