/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.spi.profiler.AbstractProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link BaseProbe} is an abstract implementation of probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class BaseProbe extends AbstractProbe {
    protected final ProbeContext context;
    protected final ThreadLocalAccessor threadLocalAccessor;

    public BaseProbe(ProbeConfiguration configuration, IProbeContext context) {
        super(configuration, context);
        this.context = (ProbeContext) context;
        this.threadLocalAccessor = this.context.getThreadLocalAccessor();
    }
}
