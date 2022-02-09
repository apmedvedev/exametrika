/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.probes;


import com.exametrika.api.metrics.exa.config.ExaInstrumentProbeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.metrics.exa.probes.ExaInstrumentProbe.CollectorInfo;
import com.exametrika.impl.profiler.probes.BaseProbeCollector;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.ThreadLocalSlot;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.aggregator.common.meters.IGauge;
import com.exametrika.spi.aggregator.common.meters.ILog;
import com.exametrika.spi.aggregator.common.meters.LogEvent;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalSlot;


/**
 * The {@link ExaInstrumentProbeCollector} is an Exa instrument probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ExaInstrumentProbeCollector extends BaseProbeCollector {
    private final ExaInstrumentProbeConfiguration configuration;
    private final ThreadLocalSlot slot;
    private ICounter transformationTime;
    private ICounter beforeTransformationBytes;
    private ICounter afterTransformationBytes;
    private ICounter skippedClasses;
    private IGauge joinPoints;
    private ILog errors;

    public ExaInstrumentProbeCollector(ExaInstrumentProbeConfiguration configuration, IProbeContext context, IScope scope,
                                       IThreadLocalSlot slot, Container container, JsonObject metadata) {
        super(configuration, context, scope, container, metadata, true, configuration.getComponentType());

        Assert.notNull(slot);
        Assert.notNull(container);

        this.slot = (ThreadLocalSlot) slot;
        this.configuration = configuration;

        createMeters();
    }

    @Override
    public void begin() {
        super.begin();

        CollectorInfo info = slot.get(false);
        info.collector = this;
    }

    @Override
    public void end() {
        CollectorInfo info = slot.get(false);
        info.collector = null;

        super.end();
    }

    public void beginMeasure() {
        transformationTime.beginMeasure(getTime());
    }

    public void endMeasure() {
        transformationTime.endMeasure(getTime());
        extract();
    }

    public void endMeasureSuccess(int beforeBytes, int afterBytes, int joinPointCount) {
        beforeTransformationBytes.measureDelta(beforeBytes);
        afterTransformationBytes.measureDelta(afterBytes);
        joinPoints.measure(joinPointCount);
    }

    public void endMeasureSkip() {
        skippedClasses.measureDelta(1);
    }

    public void endMeasureError(String className, Throwable exception) {
        long time = context.getTimeService().getCurrentTime();
        String thread = Thread.currentThread().getName();
        errors.measure(new LogEvent(errors.getId(), "error", time, null, exception, Json.object()
                .put("className", className).put("thread", thread).toObjectBuilder(), true));
    }

    @Override
    protected void createMeters() {
        transformationTime = meters.addMeter("exa.instrument.time", configuration.getTransformationTime(), null);
        beforeTransformationBytes = meters.addMeter("exa.instrument.beforeBytes", configuration.getBeforeTransformationBytes(), null);
        afterTransformationBytes = meters.addMeter("exa.instrument.afterBytes", configuration.getAfterTransformationBytes(), null);
        skippedClasses = meters.addMeter("exa.instrument.skipped", configuration.getSkippedClasses(), null);
        joinPoints = meters.addMeter("exa.instrument.joinPoints", configuration.getJoinPoints(), null);
        errors = meters.addLog("exa.instrument.errors.log", configuration.getErrors());
    }

    private long getTime() {
        if (Times.isTickCountAvaliable())
            return Times.getWallTime();
        else
            return System.nanoTime();
    }
}
