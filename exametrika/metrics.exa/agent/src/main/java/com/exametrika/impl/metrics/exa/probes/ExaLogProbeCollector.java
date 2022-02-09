/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.probes;


import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.profiler.probes.LogProbeCollector;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.ThreadLocalSlot;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalSlot;
import com.exametrika.spi.profiler.boot.ThreadLocalContainer;
import com.exametrika.spi.profiler.config.LogProbeConfiguration;


/**
 * The {@link ExaLogProbeCollector} is a exa log probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ExaLogProbeCollector extends LogProbeCollector {
    public ExaLogProbeCollector(LogProbeConfiguration configuration, IProbeContext context, IScope scope,
                                IThreadLocalSlot slot, ThreadLocalContainer container, JsonObject metadata, String componentType) {
        super(configuration, context, scope, slot, container, metadata, componentType);
    }

    @Override
    protected CollectorInfo getSlotInfo(IThreadLocalSlot slot) {
        return ((ThreadLocalSlot) slot).get(false);
    }
}
