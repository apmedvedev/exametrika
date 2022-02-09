/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;

import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeCollector;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalSlot;

public class TestProbe2 implements IProbe, IThreadLocalProvider {
    public IThreadLocalSlot slot;
    public boolean started;
    public boolean stopped;
    public boolean timered;
    public boolean enabled;
    public String probeInterceptor;

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
        return new TestProbeCollector2();
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
        return new TestThreadLocal2();
    }

    public static class TestThreadLocal2 {
    }

    @Override
    public void setEnabled(boolean value) {
        enabled = value;
    }

    @Override
    public boolean isCalibrated() {
        return true;
    }
}