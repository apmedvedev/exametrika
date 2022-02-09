/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.probes;

import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.metrics.exa.server.config.ExaAggregatorProbeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.aggregator.common.meters.IGauge;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.profiler.IProbeContext;

/**
 * The {@link ExaCacheMeterContainer} is an Exa cache meter container.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ExaCacheMeterContainer extends MeterContainer {
    private final ExaAggregatorProbeConfiguration configuration;
    private IGauge size;
    private IGauge maxSize;
    private IGauge quota;
    private ICounter totalLoaded;
    private ICounter totalUnloaded;
    private ICounter unloadedByOverflow;
    private ICounter unloadedByTimer;

    public ExaCacheMeterContainer(ExaAggregatorProbeConfiguration configuration, NameMeasurementId id, IProbeContext context,
                                  IInstanceContextProvider contextProvider, JsonObject metadata) {
        super(id, context, contextProvider);

        Assert.notNull(configuration);

        this.configuration = configuration;

        createMeters();
        setMetadata(Json.object(metadata)
                .put("node", context.getConfiguration().getNodeName())
                .toObject());
    }

    public void onCache(long cacheSize, long maxCacheSize, long quota) {
        size.measure(cacheSize);
        maxSize.measure(maxCacheSize);
        this.quota.measure(quota);
    }

    public void onLoaded() {
        totalLoaded.measureDelta(1);
    }

    public void onUnloaded(boolean byTimer) {
        totalUnloaded.measureDelta(1);
        if (!byTimer)
            unloadedByOverflow.measureDelta(1);
        else
            unloadedByTimer.measureDelta(1);
    }

    protected void createMeters() {
        String componentType = idProvider.get().getComponentType();
        size = addMeter(componentType + ".size", configuration.getGauge(), null);
        maxSize = addMeter(componentType + ".maxSize", configuration.getGauge(), null);
        quota = addMeter(componentType + ".quota", configuration.getGauge(), null);
        totalLoaded = addMeter(componentType + ".totalLoaded", configuration.getCounter(), null);
        totalUnloaded = addMeter(componentType + ".totalUnloaded", configuration.getCounter(), null);
        unloadedByOverflow = addMeter(componentType + ".unloadedByOverflow", configuration.getCounter(), null);
        unloadedByTimer = addMeter(componentType + ".unloadedByTimer", configuration.getCounter(), null);
    }
}