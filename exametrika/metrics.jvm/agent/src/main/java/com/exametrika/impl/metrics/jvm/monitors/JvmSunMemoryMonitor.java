/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.monitors;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.api.metrics.jvm.config.JvmSunMemoryMonitorConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.aggregator.common.meters.ILog;
import com.exametrika.spi.aggregator.common.meters.IMeterContainer;
import com.exametrika.spi.aggregator.common.meters.LogEvent;
import com.exametrika.spi.profiler.IMonitorContext;
import com.sun.management.GcInfo;


/**
 * The {@link JvmSunMemoryMonitor} is a monitor of memory pools of Sun/Oracle JVM.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JvmSunMemoryMonitor extends JvmMemoryMonitor {
    private final JvmSunMemoryMonitorConfiguration configuration;
    private final Map<String, MetersInfo> poolMeters = new LinkedHashMap<String, MetersInfo>();
    private final Map<String, MetersInfo> collectorMeters = new LinkedHashMap<String, MetersInfo>();
    private final long[] lastGcIds = new long[ManagementFactory.getGarbageCollectorMXBeans().size()];
    private final GcFilter filter;

    public JvmSunMemoryMonitor(JvmSunMemoryMonitorConfiguration configuration, IMonitorContext context) {
        super(configuration, context);

        this.configuration = configuration;
        if (configuration.getFilter() != null)
            filter = new GcFilter(configuration.getFilter());
        else
            filter = null;
    }

    @Override
    protected void createPoolGcMeters(String name, IMeterContainer meters) {
        MetersInfo info = new MetersInfo();

        info.time = meters.addMeter("jvm.gc.time", configuration.getTimeCounter(), null);
        info.bytes = meters.addMeter("jvm.gc.bytes", configuration.getBytesCounter(), null);
        info.stops = meters.addCounter("jvm.gc.stops", false, 0, null);
        info.log = meters.addLog("jvm.gc.log", configuration.getLog());

        poolMeters.put(name, info);
    }

    @Override
    protected void createCollectorGcMeters(String name, IMeterContainer meters) {
        MetersInfo info = new MetersInfo();

        info.time = meters.addMeter("jvm.gc.time", configuration.getTimeCounter(), null);
        info.bytes = meters.addMeter("jvm.gc.bytes", configuration.getBytesCounter(), null);
        info.stops = meters.addCounter("jvm.gc.stops", false, 0, null);

        collectorMeters.put(name, info);
    }

    @Override
    protected void doMeasure() {
        long currentTime = context.getTimeService().getCurrentTime();

        int i = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            com.sun.management.GarbageCollectorMXBean sunBean = (com.sun.management.GarbageCollectorMXBean) bean;
            GcInfo gcInfo = sunBean.getLastGcInfo();
            if (gcInfo == null) {
                i++;
                continue;
            }

            if (lastGcIds[i] == gcInfo.getId()) {
                i++;
                continue;
            }

            lastGcIds[i] = gcInfo.getId();
            i++;

            if (filter != null && !filter.evaluate(gcInfo))
                continue;

            MetersInfo collectorInfo = collectorMeters.get(bean.getName());

            if (gcInfo.getDuration() > configuration.getMaxGcDuration())
                collectorInfo.stops.measureDelta(1);

            for (Map.Entry<String, MemoryUsage> entry : gcInfo.getMemoryUsageBeforeGc().entrySet()) {
                MemoryUsage beforeUsage = entry.getValue();
                MemoryUsage afterUsage = gcInfo.getMemoryUsageAfterGc().get(entry.getKey());
                MetersInfo info = poolMeters.get(entry.getKey());

                long freedMemory = beforeUsage.getUsed() - afterUsage.getUsed();
                if (freedMemory <= 0)
                    continue;

                info.time.measureDelta(gcInfo.getDuration());
                info.bytes.measureDelta(freedMemory);
                collectorInfo.time.measureDelta(gcInfo.getDuration());
                collectorInfo.bytes.measureDelta(freedMemory);

                if (gcInfo.getDuration() > configuration.getMaxGcDuration())
                    info.stops.measureDelta(1);

                LogEvent event = new LogEvent(info.log.getId(), "log", currentTime, "", null, Json.object()
                        .put("id", gcInfo.getId())
                        .put("start", gcInfo.getStartTime())
                        .put("end", gcInfo.getEndTime())
                        .put("bytes", freedMemory)
                        .toObjectBuilder(), false);
                info.log.measure(event);
            }
        }
    }

    private static class MetersInfo {
        ICounter time;
        ICounter bytes;
        ICounter stops;
        ILog log;
    }
}
