/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;

import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeCollector;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalProviderRegistrar;
import com.exametrika.spi.profiler.IThreadLocalProviderRegistry;
import com.exametrika.spi.profiler.IThreadLocalSlot;

public class TestProbe1 implements IProbe, IThreadLocalProvider, IThreadLocalProviderRegistrar {
    public IThreadLocalSlot slot;
    public boolean started;
    public boolean stopped;
    public boolean timered;
    public String probeInterceptor;
    public boolean enabled;

    @Override
    public boolean isSystem() {
        return false;
    }

    @Override
    public boolean isStack() {
        return true;
    }

    @Override
    public void start() {
        started = true;
    }

    @Override
    public void stop() {
        stopped = true;
    }

    @Override
    public void calibrate(boolean force) {
    }

    @Override
    public boolean isProbeInterceptor(String className) {
        return className.equals(probeInterceptor);
    }

    @Override
    public IProbeCollector createCollector(IScope scope) {
        return new TestProbeCollector1();
    }

    @Override
    public void onTimer() {
        timered = true;
    }

    @Override
    public void setSlot(IThreadLocalSlot slot) {
        this.slot = slot;
    }

    @Override
    public Object allocate() {
        return new TestThreadLocal1();
    }

    @Override
    public void register(IThreadLocalProviderRegistry registry) {
        registry.addProvider(this);
    }

    public static class TestThreadLocal1 {
    }

    @Override
    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    @Override
    public boolean isCalibrated() {
        return true;
    }
}