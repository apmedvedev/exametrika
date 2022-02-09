/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.monitors;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.metrics.jvm.config.JvmThreadMonitorConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.profiler.SigarHolder;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.IMeterContainer;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link JvmThreadMonitor} is a monitor of threads of JVM.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class JvmThreadMonitor extends AbstractMonitor {
    private final JvmThreadMonitorConfiguration configuration;
    private final TLongObjectMap<IMeterContainer> threadsMeters = new TLongObjectHashMap<IMeterContainer>();

    public JvmThreadMonitor(JvmThreadMonitorConfiguration configuration, IMonitorContext context) {
        super("jvm.threads", configuration, context, false);

        this.configuration = configuration;
    }

    @Override
    public void start() {
        super.start();

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (bean.isThreadCpuTimeSupported() && !bean.isThreadCpuTimeEnabled())
            bean.setThreadCpuTimeEnabled(true);
        if ((configuration.isContention() || configuration.getLocks()) && bean.isThreadContentionMonitoringSupported() && !bean.isThreadContentionMonitoringEnabled())
            bean.setThreadContentionMonitoringEnabled(true);
    }

    @Override
    protected void createMeters() {
        final ThreadMXBean thread = ManagementFactory.getThreadMXBean();

        meters.addGauge("jvm.threads.daemons", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                return thread.getDaemonThreadCount();
            }
        });

        meters.addGauge("jvm.threads.started", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                return thread.getTotalStartedThreadCount();
            }
        });
    }

    @Override
    protected void updateMetersContainers() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] ids = bean.getAllThreadIds();
        for (int i = 0; i < ids.length; i++) {
            final long id = ids[i];
            IMeterContainer meters = threadsMeters.get(id);
            if (meters == null) {
                meters = createMeterContainer(id);
                threadsMeters.put(id, meters);
            }

            meters.setProcessed(true);
        }

        for (TLongObjectIterator<IMeterContainer> it = threadsMeters.iterator(); it.hasNext(); ) {
            it.advance();
            IMeterContainer meters = it.value();
            if (!meters.isProcessed()) {
                it.remove();
                meters.delete();
            } else
                meters.setProcessed(false);
        }
    }

    protected void createAllocatedCounter(IMeterContainer meters, long threadId) {
        meters.addCounter("jvm.thread.allocated", false, 0, null);
    }

    private IMeterContainer createMeterContainer(long id) {
        String subScope = Long.toString(id);

        MeterContainer meterContainer = new ThreadMeterContainer(getMeasurementId(subScope, MetricName.root(), "jvm.thread"),
                context, (IInstanceContextProvider) context, id);
        addMeters(meterContainer);

        return meterContainer;
    }

    private class ThreadMeterContainer extends MeterContainer {
        private final ThreadMXBean bean;
        private final long threadId;
        private ThreadInfo threadInfo;

        public ThreadMeterContainer(NameMeasurementId id, IMonitorContext context,
                                    IInstanceContextProvider contextProvider, long threadId) {
            super(id, context, contextProvider);

            this.threadId = threadId;
            this.bean = ManagementFactory.getThreadMXBean();

            createMeters();
        }

        @Override
        public void measure() {
            try {
                boolean first = threadInfo == null;
                if (!configuration.getLocks())
                    threadInfo = bean.getThreadInfo(threadId, configuration.getStackTraces() ? configuration.getMaxStackTraceDepth() : 0);
                else
                    threadInfo = bean.getThreadInfo(new long[]{threadId}, bean.isObjectMonitorUsageSupported(),
                            bean.isSynchronizerUsageSupported())[0];

                if (threadInfo != null) {
                    if (first) {
                        JsonObject metadata = Json.object()
                                .put("node", ((IMonitorContext) context).getConfiguration().getNodeName())
                                .put("name", threadInfo.getThreadName())
                                .toObject();
                        setMetadata(metadata);
                    }

                    super.measure();
                } else
                    delete();
            } catch (Exception e) {
                delete();
            }
        }

        @Override
        public void delete() {
            super.delete();

            threadsMeters.remove(threadId);
        }

        private void createMeters() {
            addInfo("jvm.thread.state", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    return threadInfo.getThreadState().toString().toLowerCase();
                }
            });

            addCounter("jvm.thread.time", false, 0, new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    return System.nanoTime() / 1000000;
                }
            });

            addCounter("jvm.thread.cpu.max", false, 0, new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    try {
                        long value = SigarHolder.instance.getCpu().getTotal();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    } catch (Exception e) {
                        return Exceptions.wrapAndThrow(e);
                    }
                }
            });

            addCounter("jvm.thread.cpu.total", false, 0, new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    if (bean.isThreadCpuTimeSupported()) {
                        long value = bean.getThreadCpuTime(threadId);
                        if (value != -1)
                            return value / 1000000;
                        else
                            return null;
                    } else
                        return null;
                }
            });

            addCounter("jvm.thread.cpu.user", false, 0, new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    if (bean.isThreadCpuTimeSupported()) {
                        long value = bean.getThreadUserTime(threadId);
                        if (value != -1)
                            return value / 1000000;
                        else
                            return null;
                    } else
                        return null;
                }
            });

            addCounter("jvm.thread.cpu.sys", false, 0, new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    if (bean.isThreadCpuTimeSupported()) {
                        long value = bean.getThreadCpuTime(threadId) - bean.getThreadUserTime(threadId);
                        if (value >= 0)
                            return value / 1000000;
                        else
                            return null;
                    } else
                        return null;
                }
            });

            addCounter("jvm.thread.time.waited", false, 0, new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    long value = threadInfo.getWaitedCount();
                    if (value != -1)
                        return new Pair(value, threadInfo.getWaitedTime());
                    else
                        return null;
                }
            });

            addCounter("jvm.thread.time.blocked", false, 0, new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    long value = threadInfo.getBlockedCount();
                    if (value != -1)
                        return new Pair(value, threadInfo.getBlockedTime());
                    else
                        return null;
                }
            });

            addInfo("jvm.thread.lock", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    if (configuration.getLocks())
                        return buildLock(threadInfo);
                    else
                        return null;
                }
            });

            addInfo("jvm.thread.locked", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    if (configuration.getLocks())
                        return buildLocked(threadInfo);
                    else
                        return null;
                }
            });

            addInfo("jvm.thread.stackTrace", new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    if (configuration.getStackTraces())
                        return buildStackTrace(threadInfo.getStackTrace());
                    else
                        return null;
                }
            });

            createAllocatedCounter(this, threadId);
        }
    }

    private JsonArray buildStackTrace(StackTraceElement[] trace) {
        Json json = Json.array();
        int count = Math.min(trace.length, configuration.getMaxStackTraceDepth());

        for (int i = 0; i < count; i++) {
            json.addObject()
                    .put("class", trace[i].getClassName())
                    .put("method", trace[i].getMethodName())
                    .put("file", trace[i].getFileName())
                    .put("line", trace[i].getLineNumber())
                    .end();
        }

        return json.toArray();
    }

    private JsonObject buildLock(ThreadInfo threadInfo) {
        LockInfo lockInfo = threadInfo.getLockInfo();
        if (lockInfo == null)
            return null;

        return Json.object()
                .put("lockName", threadInfo.getLockName())
                .putIf("ownerThreadId", threadInfo.getLockOwnerId(), threadInfo.getLockOwnerId() >= 0)
                .putIf("ownerThreadName", threadInfo.getLockOwnerName(), threadInfo.getLockOwnerName() != null)
                .toObject();
    }

    private JsonObject buildLocked(ThreadInfo threadInfo) {
        MonitorInfo[] monitors = threadInfo.getLockedMonitors();
        LockInfo[] synchronizers = threadInfo.getLockedSynchronizers();
        if (monitors.length == 0 && synchronizers.length == 0)
            return null;

        Json json = Json.object();

        if (monitors.length > 0) {
            Json jsonMonitors = json.putArray("monitors");
            for (MonitorInfo monitorInfo : threadInfo.getLockedMonitors()) {
                Json jsonMonitor = jsonMonitors.addObject();
                jsonMonitor.put("lockName", monitorInfo.toString())
                        .putIf("stackDepth", monitorInfo.getLockedStackDepth(), monitorInfo.getLockedStackDepth() >= 0)
                        .put("class", monitorInfo.getLockedStackFrame().getClassName())
                        .put("method", monitorInfo.getLockedStackFrame().getMethodName())
                        .put("file", monitorInfo.getLockedStackFrame().getFileName())
                        .put("line", monitorInfo.getLockedStackFrame().getLineNumber());
            }
        }

        if (synchronizers.length > 0) {
            Json jsonSynchronizers = json.putArray("synchronizers");
            for (LockInfo lockInfo : threadInfo.getLockedSynchronizers())
                jsonSynchronizers.add(lockInfo.toString());
        }

        return json.toObject();
    }
}
