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
 * The {@link ExaFullTextMeterContainer} is an Exa full text meter container.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ExaFullTextMeterContainer extends MeterContainer {
    private final ExaAggregatorProbeConfiguration configuration;
    private ICounter addTime;
    private ICounter updateTime;
    private ICounter deleteTime;
    private ICounter searchTime;
    private ICounter searcherUpdateTime;
    private ICounter commitTime;

    public ExaFullTextMeterContainer(ExaAggregatorProbeConfiguration configuration, NameMeasurementId id, IProbeContext context,
                                     IInstanceContextProvider contextProvider, JsonObject metadata) {
        super(id, context, contextProvider);

        Assert.notNull(configuration);

        this.configuration = configuration;

        createMeters();
        setMetadata(Json.object(metadata)
                .put("node", context.getConfiguration().getNodeName())
                .toObject());
    }

    public void onBeforeFullTextAdded() {
        addTime.beginMeasure(getTime());
    }

    public void onAfterFullTextAdded() {
        addTime.endMeasure(getTime());
    }

    public void onBeforeFullTextUpdated() {
        updateTime.beginMeasure(getTime());
    }

    public void onAfterFullTextUpdated() {
        updateTime.endMeasure(getTime());
    }

    public void onBeforeFullTextDeleted() {
        deleteTime.beginMeasure(getTime());
    }

    public void onAfterFullTextDeleted() {
        deleteTime.endMeasure(getTime());
    }

    public void onBeforeFullTextSearched() {
        searchTime.beginMeasure(getTime());
    }

    public void onAfterFullTextSearched() {
        searchTime.endMeasure(getTime());
    }

    public void onBeforeFullTextSearcherUpdated() {
        searcherUpdateTime.beginMeasure(getTime());
    }

    public void onAfterFullTextSearcherUpdated() {
        searcherUpdateTime.endMeasure(getTime());
    }

    public void onBeforeFullTextCommitted() {
        commitTime.beginMeasure(getTime());
    }

    public void onAfterFullTextCommitted() {
        commitTime.endMeasure(getTime());
    }

    protected void createMeters() {
        addTime = addMeter("exa.exadb.fullText.addTime", configuration.getTimeCounter(), null);
        updateTime = addMeter("exa.exadb.fullText.updateTime", configuration.getTimeCounter(), null);
        deleteTime = addMeter("exa.exadb.fullText.deleteTime", configuration.getTimeCounter(), null);
        searchTime = addMeter("exa.exadb.fullText.searchTime", configuration.getTimeCounter(), null);
        searcherUpdateTime = addMeter("exa.exadb.fullText.searcherUpdateTime", configuration.getTimeCounter(), null);
        commitTime = addMeter("exa.exadb.fullText.commitTime", configuration.getTimeCounter(), null);
    }

    private long getTime() {
        if (Times.isTickCountAvaliable())
            return Times.getWallTime();
        else
            return System.nanoTime();
    }
}