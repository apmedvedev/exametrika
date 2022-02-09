/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.profiler.config.MethodExitPointProbeConfiguration;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link MethodExitPointProbeCollector} is a method exit point probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MethodExitPointProbeCollector extends ExitPointProbeCollector {
    public MethodExitPointProbeCollector(MethodExitPointProbeConfiguration configuration,
                                         int index, String name, UUID stackId, ICallPath callPath, StackProbeRootCollector root,
                                         StackProbeCollector parent, JsonObject metadata, ExitPointProbeCalibrateInfo calibrateInfo, boolean leaf) {
        super(configuration, index, name, stackId, callPath, root, parent, metadata, calibrateInfo, leaf);
    }
}
