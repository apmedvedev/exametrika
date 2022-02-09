/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.profiler.AbstractExitPointProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.TraceTag;
import com.exametrika.spi.profiler.boot.ThreadLocalContainer;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;


/**
 * The {@link DelegatingExitPointProbe} is a delegating exit point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DelegatingExitPointProbe extends ExitPointProbe {
    private final AbstractExitPointProbe delegate;

    public DelegatingExitPointProbe(ExitPointProbeConfiguration configuration, IProbeContext context, int index,
                                    String name, AbstractExitPointProbe delegate) {
        super(configuration, context, index, name);

        Assert.notNull(delegate);

        this.delegate = delegate;
        delegate.setBaseProbe(this);
    }

    @Override
    public void start() {
        super.start();

        delegate.start();
    }

    @Override
    public void stop() {
        super.stop();

        delegate.stop();
    }

    public void beginRequest(ThreadLocalContainer container, Object rawRequest) {
        super.beginRequest((Container) container, rawRequest);
    }

    public void endRequest(ThreadLocalContainer container, JsonObject error, long totalDelta, long childrenTotalDelta) {
        super.endRequest((Container) container, error, totalDelta, childrenTotalDelta);
    }

    public IRequest superGetRequest() {
        return super.getRequest();
    }

    public IRequest superMapRequest(IScope scope, Object rawRequest) {
        return super.mapRequest(scope, rawRequest);
    }

    public DelegatingExitPointProbeCollector superGetCollector() {
        return super.getCollector();
    }

    public boolean superIsRecursive() {
        return super.isRecursive();
    }

    public void superSetRecursive(boolean value) {
        super.setRecursive(value);
    }

    public long superGetStartTime() {
        return super.getStartTime();
    }

    public long superGetTimeDelta(long startTime) {
        return super.getTimeDelta(startTime);
    }

    @Override
    protected ExitPointProbeCollector doCreateCollector(int index, String name, UUID stackId, ICallPath callPath,
                                                        StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata, ExitPointProbeCalibrateInfo calibrateInfo,
                                                        boolean leaf) {
        return new DelegatingExitPointProbeCollector(configuration, index, name, stackId, callPath, root, parent, metadata, calibrateInfo, leaf,
                delegate.createCollector());
    }

    @Override
    protected IRequest mapRequest(IScope scope, Object rawRequest) {
        return delegate.mapRequest(scope, rawRequest);
    }

    @Override
    protected Object createCalibratingRequest() {
        return delegate.createCalibratingRequest();
    }

    @Override
    protected void writeTag(Object request, TraceTag tag) {
        delegate.writeTag(request, tag);
    }
}
