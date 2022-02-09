/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.probes;

import com.exametrika.api.metrics.exa.config.ExaMessagingProbeConfiguration;
import com.exametrika.common.messaging.impl.protocols.trace.ChannelInterceptor;
import com.exametrika.impl.profiler.probes.BaseProbe;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.ThreadLocalSlot;
import com.exametrika.spi.profiler.IProbeCollector;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalSlot;


/**
 * The {@link ExaMessagingProbe} is a Exa messaging probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaMessagingProbe extends BaseProbe implements IThreadLocalProvider {
    private final ExaMessagingProbeConfiguration configuration;
    private ThreadLocalSlot slot;

    public ExaMessagingProbe(ExaMessagingProbeConfiguration configuration, IProbeContext context) {
        super(configuration, context);

        this.configuration = configuration;
    }

    @Override
    public boolean isSystem() {
        return true;
    }

    @Override
    public synchronized void start() {
        ChannelInterceptor.INSTANCE = new ExaChannelInterceptor(ChannelInterceptor.INSTANCE);
    }

    @Override
    public synchronized void stop() {
        ChannelInterceptor.INSTANCE = new ChannelInterceptor(ChannelInterceptor.INSTANCE);
    }

    @Override
    public boolean isStack() {
        return false;
    }

    @Override
    public IProbeCollector createCollector(IScope scope) {
        return new ExaMessagingProbeCollector(configuration, context, scope, slot, threadLocalAccessor.get(false),
                ChannelInterceptor.INSTANCE.slotAllocator);
    }

    @Override
    public void onTimer() {
    }

    @Override
    public void setSlot(IThreadLocalSlot slot) {
        this.slot = (ThreadLocalSlot) slot;
    }

    @Override
    public Object allocate() {
        return new CollectorInfo();
    }

    static class CollectorInfo {
        public ExaMessagingProbeCollector collector;
    }

    private class ExaChannelInterceptor extends ChannelInterceptor {
        public ExaChannelInterceptor(ChannelInterceptor interceptor) {
            super(interceptor);
        }

        @Override
        public void onMessageSent(int id, int messageSize) {
            Container container = threadLocalAccessor.get(false);
            if (container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.measureSend(id, messageSize);
        }

        @Override
        public void onMessageReceived(int id, int messageSize) {
            Container container = threadLocalAccessor.get(false);
            if (container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.measureReceive(id, messageSize);

        }

        @Override
        public void onNodeFailed(int id, String nodeName) {
            Container container = threadLocalAccessor.get(false);
            if (container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.measureFailure(id, nodeName);
        }
    }
}
