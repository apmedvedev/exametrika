/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;

public class TestMeasurementHandler implements IMeasurementHandler {
    public List<MeasurementSet> measurements = new ArrayList<MeasurementSet>();

    @Override
    public void handle(MeasurementSet measurements) {
        this.measurements.add(measurements);
    }

    @Override
    public boolean canHandle() {
        return true;
    }
}