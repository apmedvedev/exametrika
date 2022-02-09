/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import com.exametrika.common.utils.Times;
import com.exametrika.spi.profiler.ITimeSource;


/**
 * The {@link ThreadCpuTimeSource} is a thread cpu time source.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ThreadCpuTimeSource implements ITimeSource {
    private final ThreadMXBean bean = ManagementFactory.getThreadMXBean();

    @Override
    public long getCurrentTime() {
        return Times.getThreadCpuTime();
    }

    @Override
    public long getCurrentTime(long threadId) {
        return bean.getThreadCpuTime(threadId);
    }
}
