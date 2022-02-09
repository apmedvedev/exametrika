/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.profiler.IProbeContext;
import com.sun.management.ThreadMXBean;


/**
 * The {@link AppStackCounterProvider} is a application stack counter measurement provider.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class AppStackCounterProvider implements IMeasurementProvider {
    private static final boolean supportsAllocationBytes;
    private final ThreadLocalAccessor threadLocalAccessor;
    private final AppStackCounterType type;

    static {
        boolean value = false;
        try {
            value = ManagementFactory.getThreadMXBean() instanceof ThreadMXBean;
        } catch (Throwable e) {
        }

        supportsAllocationBytes = value;
    }

    public AppStackCounterProvider(AppStackCounterType counterType, IProbeContext context) {
        Assert.notNull(counterType);

        this.threadLocalAccessor = ((ProbeContext) context).getThreadLocalAccessor();
        this.type = counterType;

        if (counterType.isSystem())
            enableCounter();
    }

    @Override
    public Object getValue() throws Exception {
        if (!type.isSystem()) {
            Container container = threadLocalAccessor.get();
            if (container != null)
                return container.counters[type.ordinal()];
            else
                return 0l;
        } else
            return getSystemCounter();
    }

    private long getSystemCounter() {
        switch (type) {
            case WALL_TIME:
                return System.nanoTime();
            case SYS_TIME: {
                java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                if (bean.isCurrentThreadCpuTimeSupported())
                    return bean.getCurrentThreadCpuTime() - bean.getCurrentThreadUserTime();
                else
                    return 0;
            }
            case USER_TIME: {
                java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                if (bean.isCurrentThreadCpuTimeSupported())
                    return bean.getCurrentThreadUserTime();
                else
                    return 0;
            }
            case WAIT_TIME: {
                java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                if (bean.isThreadContentionMonitoringSupported())
                    return bean.getThreadInfo(Thread.currentThread().getId()).getWaitedTime() * 1000000;
                else
                    return 0;
            }
            case WAIT_COUNT: {
                java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                if (bean.isThreadContentionMonitoringSupported())
                    return bean.getThreadInfo(Thread.currentThread().getId()).getWaitedCount();
                else
                    return 0;
            }
            case BLOCK_TIME: {
                java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                if (bean.isThreadContentionMonitoringSupported())
                    return bean.getThreadInfo(Thread.currentThread().getId()).getBlockedTime() * 1000000;
                else
                    return 0;
            }
            case BLOCK_COUNT: {
                java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                if (bean.isThreadContentionMonitoringSupported())
                    return bean.getThreadInfo(Thread.currentThread().getId()).getBlockedCount();
                else
                    return 0;
            }
            case GARBAGE_COLLECTION_COUNT: {
                long count = 0;
                for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans())
                    count = bean.getCollectionCount();
                return count;
            }
            case GARBAGE_COLLECTION_TIME: {
                long time = 0;
                for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans())
                    time = bean.getCollectionTime() * 1000000;
                return time;
            }
            case ALLOCATION_BYTES:
                if (supportsAllocationBytes)
                    return getAllocationBytes();
                else
                    return 0;
            default:
                return 0;
        }
    }

    private void enableCounter() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        switch (type) {
            case USER_TIME:
            case SYS_TIME:
                if (bean.isCurrentThreadCpuTimeSupported())
                    ManagementFactory.getThreadMXBean().setThreadCpuTimeEnabled(true);
                break;
            case WAIT_TIME:
            case WAIT_COUNT:
            case BLOCK_TIME:
            case BLOCK_COUNT:
                if (bean.isThreadContentionMonitoringSupported())
                    bean.setThreadContentionMonitoringEnabled(true);
                break;
            case ALLOCATION_BYTES:
                if (supportsAllocationBytes)
                    enableAllocationBytes();
        }
    }

    private void enableAllocationBytes() {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (bean.isThreadAllocatedMemorySupported())
            bean.setThreadAllocatedMemoryEnabled(true);
    }

    private long getAllocationBytes() {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (bean.isThreadAllocatedMemorySupported())
            return bean.getThreadAllocatedBytes(Thread.currentThread().getId());
        else
            return 0;
    }
}
