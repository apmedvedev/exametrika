/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.services;

import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.spi.aggregator.IAggregationService;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.IMeasurementContext;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;

public class ComponentMeasurementContext implements IMeasurementContext, IInstanceContextProvider {
    private final IAggregationService aggregationService;
    private final IMeasurementHandler measurementHandler = new MeasurementHander();
    private final int schemaVersion;
    private JsonObject context;

    public ComponentMeasurementContext(IAggregationService aggregationService, int schemaVersion) {
        Assert.notNull(aggregationService);

        this.aggregationService = aggregationService;
        this.schemaVersion = schemaVersion;
    }

    @Override
    public JsonObject getContext() {
        return context;
    }

    @Override
    public void setContext(JsonObject context) {
        this.context = context;
    }

    @Override
    public long getExtractionTime() {
        return Times.getCurrentTime();
    }

    @Override
    public int getSchemaVersion() {
        return schemaVersion;
    }

    @Override
    public IMeasurementHandler getMeasurementHandler() {
        return measurementHandler;
    }

    private class MeasurementHander implements IMeasurementHandler {
        @Override
        public boolean canHandle() {
            return true;
        }

        @Override
        public void handle(MeasurementSet measurements) {
            aggregationService.aggregate(measurements);
        }
    }
}