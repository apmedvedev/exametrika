/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.common.json.JsonObject;
import com.exametrika.spi.profiler.IRequest;


/**
 * The {@link ThreadEntryPointProbeCollector} is a thread entry point probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ThreadEntryPointProbeCollector extends EntryPointProbeCollector {
    public ThreadEntryPointProbeCollector(int index,
                                          ThreadEntryPointProbe probe, String name, String combineId, ICallPath callPath, StackProbeRootCollector root,
                                          StackProbeCollector parent, JsonObject metadata, boolean primary, boolean leaf) {
        super(index, probe, name, combineId, callPath, root, parent, metadata, primary, leaf);
    }

    @Override
    protected void doEndMeasure(IRequest request, long currentThreadCpuTime) {
        ThreadRequest threadRequest = (ThreadRequest) request.getRawRequest();

        if (timeCounter != null)
            timeCounter.measureDelta(threadRequest.getEndTime() - threadRequest.getStartTime());
    }
}
