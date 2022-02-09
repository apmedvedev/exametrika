/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.monitors;

import java.util.List;

import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.profiler.IDumpProvider;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link MonitorSet} is a set of monitors.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MonitorSet implements IMonitor, IDumpProvider {
    private final MonitorConfiguration configuration;
    private final List<IMonitor> monitors;

    public MonitorSet(MonitorConfiguration configuration, List<IMonitor> monitors) {
        Assert.notNull(configuration);
        Assert.notNull(monitors);
        Assert.isTrue(!monitors.isEmpty());

        this.configuration = configuration;
        this.monitors = monitors;
    }

    @Override
    public void start() {
        for (IMonitor monitor : monitors)
            monitor.start();
    }

    @Override
    public void stop() {
        for (IMonitor monitor : monitors)
            monitor.stop();
    }

    @Override
    public void measure(List<Measurement> measurements, long time, long period, boolean force) {
        for (IMonitor monitor : monitors)
            monitor.measure(measurements, time, period, force);
    }

    @Override
    public String getName() {
        return configuration.getName();
    }

    @Override
    public JsonObject dump(int flags) {
        JsonObjectBuilder builder = new JsonObjectBuilder();
        for (IMonitor monitor : monitors) {
            if (!(monitor instanceof IDumpProvider))
                continue;

            IDumpProvider dumpProvider = (IDumpProvider) monitor;
            builder.put(dumpProvider.getName(), dumpProvider.dump(flags));
        }

        return builder.toJson();
    }
}
