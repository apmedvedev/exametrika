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
 * The {@link ExaPageTypeMeterContainer} is an Exa page type meter container.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ExaPageTypeMeterContainer extends MeterContainer {
    private final ExaAggregatorProbeConfiguration configuration;
    private IGauge reqionsCount;
    private IGauge regionsSize;
    private ICounter allocated;
    private ICounter freed;

    public ExaPageTypeMeterContainer(ExaAggregatorProbeConfiguration configuration, NameMeasurementId id, IProbeContext context,
                                     IInstanceContextProvider contextProvider, JsonObject metadata) {
        super(id, context, contextProvider);

        Assert.notNull(configuration);

        this.configuration = configuration;

        createMeters();
        setMetadata(Json.object(metadata)
                .put("node", context.getConfiguration().getNodeName())
                .toObject());
    }

    public void onPageType(long currentRegionsCount, long currentRegionsSize) {
        reqionsCount.measure(currentRegionsCount);
        regionsSize.measure(currentRegionsSize);
    }

    public void onRegionAllocated(int size) {
        allocated.measureDelta(size);
    }

    public void onRegionFreed(int size) {
        freed.measureDelta(size);
    }

    protected void createMeters() {
        reqionsCount = addMeter("exa.rawdb.pageType.regionsCount", configuration.getGauge(), null);
        regionsSize = addMeter("exa.rawdb.pageType.regionsSize", configuration.getGauge(), null);
        allocated = addMeter("exa.rawdb.pageType.allocated", configuration.getCounter(), null);
        freed = addMeter("exa.rawdb.pageType.freed", configuration.getCounter(), null);
    }
}