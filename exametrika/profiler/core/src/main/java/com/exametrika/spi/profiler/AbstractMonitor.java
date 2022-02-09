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
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.IMeterContainer;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link AbstractMonitor} is an abstract implementation of monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AbstractMonitor implements IMonitor, IDumpProvider {
    protected final MonitorConfiguration configuration;
    protected final IMonitorContext context;
    protected final IMeterContainer meters;
    private final boolean useThreadPool;
    private final SimpleList<MeterContainer> meterContainers = new SimpleList<MeterContainer>();

    public AbstractMonitor(String componentType, MonitorConfiguration configuration, IMonitorContext context, boolean useThreadPool) {
        Assert.notNull(configuration);
        Assert.notNull(context);

        this.configuration = configuration;
        this.context = context;
        this.useThreadPool = useThreadPool;

        if (componentType != null) {
            meters = createMeterContainer(null, Names.rootMetric(), componentType);
            initMetadata(meters);
        } else
            meters = null;
    }

    @Override
    public synchronized void start() {
        createMeters();
    }

    @Override
    public void stop() {
    }

    @Override
    public void measure(List<Measurement> measurements, final long time, final long period, final boolean force) {
        if (!useThreadPool)
            measure(measurements, period, force);
        else {
            context.getTaskQueue().offer(new Runnable() {
                @Override
                public void run() {
                    int schemaVersion = context.getConfiguration().getSchemaVersion();
                    List<Measurement> list = new ArrayList<Measurement>();
                    measure(list, period, force);

                    MeasurementSet measurements = new MeasurementSet(list, null, schemaVersion, time, 0);
                    context.getMeasurementHandler().handle(measurements);
                }
            });
        }
    }

    @Override
    public String getName() {
        return configuration.getName();
    }

    @Override
    public synchronized JsonObject dump(int flags) {
        if ((flags & IProfilerMXBean.MEASUREMENTS_FLAG) != 0) {
            Json json = Json.object().putArray("meters");
            for (MeterContainer meterContainer : meterContainers.values())
                meterContainer.dump(json, false, 0);
            return json.end().toObject();
        }

        return null;
    }

    protected abstract void createMeters();

    protected void initMetadata(IMeterContainer meters) {
        JsonObject metadata = Json.object()
                .put("node", context.getConfiguration().getNodeName())
                .toObject();
        meters.setMetadata(metadata);
    }

    protected void doMeasure() {
    }

    protected void updateMetersContainers() {
    }

    protected final NameMeasurementId getMeasurementId(String subScope, IMetricName metricName, String componentType) {
        return new NameMeasurementId(Names.getScope(context.getConfiguration().getNodeName() +
                (!Strings.isEmpty(configuration.getScope()) ? ("." + configuration.getScope()) : "") +
                (!Strings.isEmpty(subScope) ? ("." + subScope) : "")), metricName, componentType);
    }

    protected IMeterContainer createMeterContainer(String subScope, IMetricName metricName, String componentType) {
        MeterContainer meterContainer = new MeterContainer(getMeasurementId(subScope, metricName, componentType),
                context, (IInstanceContextProvider) context);
        addMeters(meterContainer);

        return meterContainer;
    }

    protected final void addMeters(MeterContainer meterContainer) {
        meterContainers.addLast(meterContainer.getElement());
    }

    private synchronized void measure(List<Measurement> measurements, long period, boolean force) {
        updateMetersContainers();
        doMeasure();

        for (MeterContainer meterContainer : meterContainers.values()) {
            meterContainer.measure();

            Measurement measurement = meterContainer.extract(period, 0, force, true);
            if (measurement != null)
                measurements.add(measurement);
        }
    }
}
