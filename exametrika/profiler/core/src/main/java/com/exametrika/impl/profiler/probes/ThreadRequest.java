/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.impl.profiler.boot.ThreadEntryPointProbeInterceptor;
import com.exametrika.spi.profiler.Request;
import com.exametrika.spi.profiler.TraceTag;

/**
 * The {@link ThreadRequest} is a thread request.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ThreadRequest extends Request implements Runnable {
    private final Runnable task;
    private TraceTag tag;
    private long startTime;
    private long endTime;

    public ThreadRequest(Runnable task) {
        super("threadRequest", null);

        this.task = task;
    }

    public TraceTag getTag() {
        return tag;
    }

    public void setTag(TraceTag tag) {
        this.tag = tag;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public void run() {
        Object param = null;
        if (tag != null)
            param = ThreadEntryPointProbeInterceptor.onEnter(0, 0, null, new Object[]{this});

        try {
            if (task != null)
                task.run();

            if (tag != null)
                ThreadEntryPointProbeInterceptor.onReturnExit(0, 0, param, null, null);
        } catch (Throwable e) {
            if (tag != null)
                ThreadEntryPointProbeInterceptor.onThrowExit(0, 0, param, null, e);

            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else if (e instanceof Error)
                throw (Error) e;
            else
                throw new RuntimeException(e);
        }
    }
}