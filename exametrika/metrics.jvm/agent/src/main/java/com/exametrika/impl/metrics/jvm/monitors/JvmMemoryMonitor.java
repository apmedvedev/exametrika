/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.monitors;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;

import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.IMeterContainer;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link JvmMemoryMonitor} is a monitor of memory pools of JVM.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class JvmMemoryMonitor extends AbstractMonitor {
    public JvmMemoryMonitor(MonitorConfiguration configuration, IMonitorContext context) {
        super(null, configuration, context, false);
    }

    @Override
    protected void createMeters() {
        for (final MemoryPoolMXBean bean : ManagementFactory.getMemoryPoolMXBeans()) {
            IMeterContainer meters = createMeterContainer(bean.getName(), MetricName.root(), "jvm.pool");
            initMetadata(meters, bean);

            meters.addGauge("jvm.memory.pool.init", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    long value = bean.getUsage().getInit();
                    if (value != -1)
                        return value;
                    else
                        return null;
                }
            });

            meters.addGauge("jvm.memory.pool.committed", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    return bean.getUsage().getCommitted();
                }
            });

            meters.addGauge("jvm.memory.pool.used", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    return bean.getUsage().getUsed();
                }
            });

            meters.addGauge("jvm.memory.pool.max", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    MemoryUsage usage = bean.getUsage();
                    if (getPoolType(bean.getName(), bean.getType() == MemoryType.HEAP).equals("survivor") && usage.getMax() == usage.getCommitted())
                        return null;

                    long value = usage.getMax();
                    if (value != -1)
                        return value;
                    else
                        return null;
                }
            });

            meters.addCounter("host.process.time", false, 0, new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    return System.nanoTime() / 1000000;
                }
            });

            createPoolGcMeters(bean.getName(), meters);
        }

        for (final GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            IMeterContainer meters = createMeterContainer(Names.escape(bean.getName()), MetricName.root(), "jvm.gc");
            initMetadata(meters);

            meters.addCounter("host.process.time", false, 0, new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    return System.nanoTime() / 1000000;
                }
            });

            meters.addCounter("jvm.gc.collectionTime", false, 0, new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    long value = bean.getCollectionTime();
                    if (value != -1)
                        return value;
                    else
                        return null;
                }
            });

            meters.addCounter("jvm.gc.collectionCount", false, 0, new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    long value = bean.getCollectionCount();
                    if (value != -1)
                        return value;
                    else
                        return null;
                }
            });

            createCollectorGcMeters(bean.getName(), meters);
        }
    }

    protected void createPoolGcMeters(String name, IMeterContainer meters) {
        meters.addCounter("jvm.gc.time", false, 0, null);
        meters.addCounter("jvm.gc.bytes", false, 0, null);
        meters.addCounter("jvm.gc.stops", false, 0, null);
    }

    protected void createCollectorGcMeters(String name, IMeterContainer meters) {
        meters.addCounter("jvm.gc.time", false, 0, null);
        meters.addCounter("jvm.gc.bytes", false, 0, null);
        meters.addCounter("jvm.gc.stops", false, 0, null);
    }

    private String getPoolType(String beanName, boolean heap) {
        beanName = beanName.toLowerCase();

        if (heap) {
            if (beanName.contains("eden") || beanName.contains("nursery"))
                return "eden";
            else if (beanName.contains("survivor"))
                return "survivor";
            else
                return "old";
        } else {
            if (beanName.contains("compressed class"))
                return "class2";
            else if (beanName.contains("class") || beanName.contains("perm") || beanName.contains("metaspace"))
                return "class";
            else
                return "code";
        }
    }

    private void initMetadata(IMeterContainer meters, MemoryPoolMXBean bean) {
        JsonObject metadata = Json.object()
                .put("node", context.getConfiguration().getNodeName())
                .put("name", bean.getName())
                .put("type", (bean.getType() == MemoryType.HEAP ? "heap" : "non-heap") + ", " + getPoolType(bean.getName(), bean.getType() == MemoryType.HEAP))
                .toObject();
        meters.setMetadata(metadata);
    }
}
