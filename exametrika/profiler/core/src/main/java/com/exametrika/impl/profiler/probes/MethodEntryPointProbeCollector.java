/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link MethodEntryPointProbeCollector} is a method entry point probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MethodEntryPointProbeCollector extends EntryPointProbeCollector {
    public MethodEntryPointProbeCollector(int index,
                                          MethodEntryPointProbe probe, String name, String combineId, ICallPath callPath, StackProbeRootCollector root,
                                          StackProbeCollector parent, JsonObject metadata, boolean primary, boolean leaf) {
        super(index, probe, name, combineId, callPath, root, parent, metadata, primary, leaf);
    }
}
