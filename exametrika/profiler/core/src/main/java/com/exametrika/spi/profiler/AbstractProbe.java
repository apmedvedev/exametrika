/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.probes.ProbeContext;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.instrument.intercept.Interceptor;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link AbstractProbe} is an abstract implementation of probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AbstractProbe extends Interceptor implements IProbe, IDumpProvider {
    protected final ProbeConfiguration configuration;
    protected final IProbeContext context;
    protected final IThreadLocalAccessor threadLocalAccessor;
    protected volatile boolean enabled = true;

    public AbstractProbe(ProbeConfiguration configuration, IProbeContext context) {
        Assert.notNull(configuration);
        Assert.notNull(context);

        this.configuration = configuration;
        this.context = context;
        this.threadLocalAccessor = ((ProbeContext) this.context).getThreadLocalAccessor();
    }

    @Override
    public boolean isSystem() {
        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isSuspended() {
        Container container = ((ThreadLocalAccessor) threadLocalAccessor).find();
        if (container != null && container.suspended)
            return true;
        else
            return false;
    }

    @Override
    public String getName() {
        return configuration.getName();
    }

    @Override
    public JsonObject dump(int flags) {
        return null;
    }

    @Override
    public boolean isProbeInterceptor(String className) {
        return getClass().getName().equals(className);
    }

    @Override
    public boolean isCalibrated() {
        return true;
    }

    @Override
    public void calibrate(boolean force) {
    }

    @Override
    public synchronized void setEnabled(boolean value) {
        enabled = value;

        if (value)
            start();
        else
            stop();
    }

    @Override
    public void log(String message) {
        ((ThreadLocalAccessor) threadLocalAccessor).log(message);
    }

    @Override
    public void logError(Throwable exception) {
        ((ThreadLocalAccessor) threadLocalAccessor).logError(exception);
    }

    protected IThreadLocalAccessor getThreadLocalAccessor() {
        return threadLocalAccessor;
    }
}
