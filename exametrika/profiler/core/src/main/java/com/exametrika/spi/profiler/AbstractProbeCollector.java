/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.profiler.IProfilerMXBean;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.profiler.boot.ThreadLocalContainer;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link AbstractProbeCollector} is an abstract implementation of probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class AbstractProbeCollector implements IProbeCollector, IDumpProvider {
    protected final ProbeConfiguration configuration;
    protected final IProbeContext context;
    protected final IScope scope;
    private final ThreadLocalContainer container;
    protected final MeterContainer meters;
    protected final String componentType;
    private final SimpleList<MeterContainer> meterContainers = new SimpleList<MeterContainer>();
    private long nextExtractionTime;
    private long nextFullExtractionTime;
    private volatile long lastExtractionTime;

    public AbstractProbeCollector(ProbeConfiguration configuration, IProbeContext context, IScope scope, ThreadLocalContainer container,
                                  JsonObject metadata, boolean createMeters, String componentType) {
        Assert.notNull(configuration);
        Assert.notNull(context);
        Assert.notNull(scope);
        Assert.notNull(container);

        this.configuration = configuration;
        this.context = context;
        this.scope = scope;
        this.container = container;
        this.componentType = componentType;

        long currentTime = context.getTimeService().getCurrentTime();
        long extractionPeriod = configuration.getExtractionPeriod();
        lastExtractionTime = currentTime;
        nextExtractionTime = (currentTime / extractionPeriod + 1) * extractionPeriod;

        if (createMeters) {
            Assert.notNull(componentType);
            meters = createMeterContainer(null, Names.rootMetric(), componentType);
            meters.setAlwaysExtractMetadata();
            meters.setMetadata(metadata);
        } else
            meters = null;
    }

    @Override
    public boolean isExtractionRequired() {
        return context.getMeasurementHandler().canHandle() && (context.getTimeService().getCurrentTime() - lastExtractionTime > configuration.getExtractionPeriod());
    }

    @Override
    public void extract() {
        long currentTime = context.getTimeService().getCurrentTime();

        if (currentTime >= nextExtractionTime && context.getMeasurementHandler().canHandle()) {
            boolean force = false;
            if (nextFullExtractionTime > 0 && currentTime >= nextFullExtractionTime)
                force = true;

            long fullExtractionPeriod = context.getConfiguration().getFullExtractionPeriod();
            nextFullExtractionTime = (currentTime / fullExtractionPeriod + 1) * fullExtractionPeriod;

            long extractionPeriod = configuration.getExtractionPeriod();
            nextExtractionTime = (currentTime / extractionPeriod + 1) * extractionPeriod;
            long period = (lastExtractionTime != 0) ? currentTime - lastExtractionTime : extractionPeriod;

            int schemaVersion = context.getConfiguration().getSchemaVersion();
            lastExtractionTime = currentTime;

            updateMetersContainers(false);

            List<Measurement> measurements = new ArrayList<Measurement>();
            for (MeterContainer meterContainer : meterContainers.values()) {
                Measurement measurement = meterContainer.extract(period, 0, force, true);
                if (measurement != null)
                    measurements.add(measurement);
            }
            if (!measurements.isEmpty()) {
                MeasurementSet set = new MeasurementSet(measurements, null, schemaVersion, currentTime, 0);
                context.getMeasurementHandler().handle(set);
            }
        }
    }

    @Override
    public void begin() {
    }

    @Override
    public void end() {
        extract();
    }

    @Override
    public String getName() {
        return configuration.getName();
    }

    @Override
    public JsonObject dump(int flags) {
        if ((flags & IProfilerMXBean.MEASUREMENTS_FLAG) != 0) {
            Json json = Json.object();
            meters.dump(json.putArray("meters"), true, 0);
            return json.toObject();
        }

        return null;
    }

    protected abstract void createMeters();

    protected void updateMetersContainers(boolean force) {
    }

    protected final NameMeasurementId getMeasurementId(String subScope, IMetricName metricName, String componentType) {
        return new NameMeasurementId(!Strings.isEmpty(subScope) ? Names.getScope(scope.getName() + "." + subScope) : scope.getName(),
                metricName, componentType);
    }

    protected MeterContainer createMeterContainer(String subScope, IMetricName metricName, String componentType) {
        MeterContainer meterContainer = new MeterContainer(getMeasurementId(subScope, metricName, componentType),
                context, ((Container) container).contextProvider);
        addMeters(meterContainer);

        return meterContainer;
    }

    protected final void addMeters(MeterContainer meterContainer) {
        meterContainers.addLast(meterContainer.getElement());
    }

    protected ThreadLocalContainer getThreadLocalContainer() {
        return container;
    }
}
