/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.probes;

import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.metrics.exa.server.config.ExaAggregatorProbeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.profiler.IProbeContext;

/**
 * The {@link ExaAggregatorMeterContainer} is an Exa aggregator meter container.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ExaAggregatorMeterContainer extends MeterContainer {
    private final ExaAggregatorProbeConfiguration configuration;
    private ICounter aggregateTime;
    private ICounter aggregateCount;
    private ICounter closePeriodTime;
    private ICounter selectTime;
    private ICounter selectSize;

    public ExaAggregatorMeterContainer(ExaAggregatorProbeConfiguration configuration, NameMeasurementId id, IProbeContext context,
                                       IInstanceContextProvider contextProvider, JsonObject metadata) {
        super(id, context, contextProvider);

        Assert.notNull(configuration);

        this.configuration = configuration;

        createMeters();
        setMetadata(Json.object(metadata)
                .put("node", context.getConfiguration().getNodeName())
                .toObject());
    }

    public void onBeforeAggregated() {
        aggregateTime.beginMeasure(getTime());
    }

    public void onAfterAggregated(int measurementsCount) {
        aggregateTime.endMeasure(getTime());
        aggregateCount.measureDelta(measurementsCount);
    }

    public void onBeforePeriodClosed() {
        closePeriodTime.beginMeasure(getTime());
    }

    public void onAfterPeriodClosed() {
        closePeriodTime.endMeasure(getTime());
    }

    public void onSelected(long selectedTime, int selectedSize) {
        selectTime.measureDelta(selectedTime);
        selectSize.measureDelta(selectedSize);
    }

    protected void createMeters() {
        aggregateTime = addMeter("exa.aggregator.aggregateTime", configuration.getTimeCounter(), null);
        aggregateCount = addMeter("exa.aggregator.aggregateCount", configuration.getCounter(), null);
        closePeriodTime = addMeter("exa.aggregator.closePeriodTime", configuration.getTimeCounter(), null);
        selectTime = addMeter("exa.aggregator.selectTime", configuration.getTimeCounter(), null);
        selectSize = addMeter("exa.aggregator.selectSize", configuration.getCounter(), null);
    }

    private long getTime() {
        if (Times.isTickCountAvaliable())
            return Times.getWallTime();
        else
            return System.nanoTime();
    }
}