/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.profiler.AbstractProbeCollector;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.boot.ThreadLocalContainer;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link BaseProbeCollector} is an abstract implementation of probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class BaseProbeCollector extends AbstractProbeCollector {
    protected final Container container;

    public BaseProbeCollector(ProbeConfiguration configuration, IProbeContext context, IScope scope, ThreadLocalContainer container,
                              JsonObject metadata, boolean createMeters, String componentType) {
        super(configuration, context, scope, container, metadata, createMeters, componentType);

        this.container = (Container) container;
    }
}
