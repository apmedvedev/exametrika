/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.monitors;

import java.io.File;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.metrics.jvm.config.JvmKpiMonitorConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.impl.metrics.host.monitors.HostCurrentProcessMonitor;
import com.exametrika.impl.metrics.host.monitors.ProcessMeterContainer;
import com.exametrika.impl.profiler.SigarHolder;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.IMeterContainer;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.Probes;
import com.sun.management.GcInfo;


/**
 * The {@link JvmKpiMonitor} is a monitor of key performance indicators (KPIs) of JVM.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JvmKpiMonitor extends HostCurrentProcessMonitor {
    private final long[] lastGcIds = new long[ManagementFactory.getGarbageCollectorMXBeans().size()];

    public JvmKpiMonitor(JvmKpiMonitorConfiguration configuration, IMonitorContext context) {
        super(configuration, context, configuration.getComponentType());
    }

    @Override
    protected IMeterContainer createMeterContainer(String subScope, IMetricName metricName, String componentType) {
        long processId = SigarHolder.instance.getPid();
        ProcessMeterContainer meterContainer = new JvmKpiMeterContainer(getMeasurementId(subScope, metricName, componentType),
                context, (IInstanceContextProvider) context, processId);
        addMeters(meterContainer);

        return meterContainer;
    }

    private class JvmKpiMeterContainer extends ProcessMeterContainer {
        public JvmKpiMeterContainer(NameMeasurementId id, IMonitorContext context,
                                    IInstanceContextProvider contextProvider, long processId) {
            super(id, context, contextProvider, processId);
        }

        @Override
        protected void createMeters() {
            super.createMeters();

            final ThreadMXBean thread = ManagementFactory.getThreadMXBean();

            addGauge("jvm.threads.total", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    return thread.getThreadCount();
                }
            });

            final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();

            addGauge("jvm.memory.heap.init", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    long value = memory.getHeapMemoryUsage().getInit();
                    if (value != -1)
                        return value;
                    else
                        return null;
                }
            });

            addGauge("jvm.memory.heap.committed", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    return memory.getHeapMemoryUsage().getCommitted();
                }
            });

            addGauge("jvm.memory.heap.used", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    return memory.getHeapMemoryUsage().getUsed();
                }
            });

            addGauge("jvm.memory.heap.max", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    long value = memory.getHeapMemoryUsage().getMax();
                    if (value != -1)
                        return value;
                    else
                        return null;
                }
            });

            addGauge("jvm.memory.nonHeap.init", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    long value = memory.getNonHeapMemoryUsage().getInit();
                    if (value != -1)
                        return value;
                    else
                        return null;
                }
            });

            addGauge("jvm.memory.nonHeap.committed", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    return memory.getNonHeapMemoryUsage().getCommitted();
                }
            });

            addGauge("jvm.memory.nonHeap.used", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    return memory.getNonHeapMemoryUsage().getUsed();
                }
            });

            addGauge("jvm.memory.nonHeap.max", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    long value = memory.getNonHeapMemoryUsage().getMax();
                    if (value != -1)
                        return value;
                    else {
                        value = memory.getHeapMemoryUsage().getMax();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                }
            });
            addGauge("jvm.memory.buffer.used", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    long memoryUsed = 0;
                    for (final BufferPoolMXBean bean : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class))
                        memoryUsed += bean.getTotalCapacity();

                    return memoryUsed;
                }
            });
            addGauge("jvm.memory.buffer.total", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    long totalCapacity = 0;
                    for (final BufferPoolMXBean bean : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class))
                        totalCapacity += bean.getTotalCapacity();

                    return totalCapacity;
                }
            });
            addGauge("jvm.memory.buffer.max", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    if (JvmBufferPoolMonitor.maxDirectMemory != -1)
                        return JvmBufferPoolMonitor.maxDirectMemory;
                    else {
                        long value = memory.getHeapMemoryUsage().getMax();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                }
            });
            addCounter("jvm.gc.collectionTime", false, 0, new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    long collectionTime = 0;
                    for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans())
                        collectionTime += bean.getCollectionTime();

                    if (collectionTime > 0)
                        return collectionTime;
                    else
                        return null;
                }
            });

            if (Probes.isInstanceOf(ManagementFactory.getGarbageCollectorMXBeans().iterator().next(), "com.sun.management.GarbageCollectorMXBean")) {
                addCounter("jvm.gc.stops", true, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() {
                        int i = 0;
                        int count = 0;
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

                            if (gcInfo.getDuration() > ((JvmKpiMonitorConfiguration) configuration).getMaxGcDuration())
                                count++;
                        }

                        return count;
                    }
                });
            } else
                addCounter("jvm.gc.errors", false, 0, null);
        }

        @Override
        protected void initMetadata(Json json) {
            RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();

            json.put("name", bean.getName());
            json.put("host", System.getProperty("com.exametrika.hostName"));
            json.put("vmName", bean.getVmName());
            json.put("vmVendor", bean.getVmVendor());
            json.put("vmHome", System.getProperty("java.home"));
            json.put("vmVersion", System.getProperty("java.version"));
            json.put("classPath", classPathToJson(bean.getClassPath()));
            json.put("libraryPath", classPathToJson(bean.getLibraryPath()));
            if (bean.isBootClassPathSupported())
                json.put("bootClassPath", classPathToJson(bean.getBootClassPath()));
            json.put("vmArgs", JsonUtils.toJson(bean.getInputArguments()));
            json.put("systemProperties", JsonUtils.toJson(bean.getSystemProperties()));
            json.put("nodeProperties", ((IMonitorContext) context).getConfiguration().getNodeProperties());
        }

        private JsonArray classPathToJson(String classPath) {
            String[] paths = classPath.split(File.pathSeparator);
            return JsonUtils.toJson(paths);
        }
    }
}
