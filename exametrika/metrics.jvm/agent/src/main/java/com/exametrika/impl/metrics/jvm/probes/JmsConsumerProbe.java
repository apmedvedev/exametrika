/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.util.Collections;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.metrics.jvm.config.JmsConsumerProbeConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.metrics.jvm.boot.JmsConsumerProbeInterceptor;
import com.exametrika.impl.profiler.bridge.BridgeHolder;
import com.exametrika.impl.profiler.probes.EntryPointProbe;
import com.exametrika.impl.profiler.probes.EntryPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.metrics.jvm.IJmsConsumerRawRequest;
import com.exametrika.spi.metrics.jvm.boot.IJmsConsumerBridge;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.Request;
import com.exametrika.spi.profiler.TraceTag;


/**
 * The {@link JmsConsumerProbe} is a JMS consumer probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JmsConsumerProbe extends EntryPointProbe {
    private final JmsConsumerProbeConfiguration configuration;
    private final BridgeHolder<IJmsConsumerBridge> bridgeHolder;

    public class JmsConsumerRawRequest implements IJmsConsumerRawRequest {
        private final Object request;
        private final String destinationName;
        private final String destinationType;
        private final int size;
        private final TraceTag tag;
        private long startTime;
        private long endTime;

        public JmsConsumerRawRequest(Object request, String destinationName, String destinationType, int size, TraceTag tag) {
            Assert.notNull(request);
            Assert.notNull(destinationName);
            Assert.notNull(destinationType);

            this.request = request;
            this.destinationName = destinationName;
            this.destinationType = destinationType;
            this.size = size;
            this.tag = tag;
        }

        public int getSize() {
            return size;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        @Override
        public String getDestinationType() {
            return destinationType;
        }

        @Override
        public String getDestinationName() {
            return destinationName;
        }

        @Override
        public Object getProperty(String name) {
            IJmsConsumerBridge bridge = bridgeHolder.get(request);
            return bridge.getProperty(name, request);
        }
    }

    public JmsConsumerProbe(JmsConsumerProbeConfiguration configuration, IProbeContext context, int index) {
        super(configuration, context, index);

        this.configuration = configuration;

        this.bridgeHolder = new BridgeHolder<IJmsConsumerBridge>(IJmsConsumerBridge.class,
                Collections.singleton("javax.jms.Message"), Integer.MAX_VALUE, context.getTimeService());
    }

    @Override
    public synchronized void start() {
        JmsConsumerProbeInterceptor.interceptor = this;
    }

    @Override
    public synchronized void stop() {
        JmsConsumerProbeInterceptor.interceptor = null;
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || isRecursive())
            return null;

        container.inCall = true;
        container.scopes.deactivateAll();

        Object res = null;
        JmsConsumerRawRequest request = createRequest(container, params[0]);
        if (request != null) {
            beginRequest(container, request, request.tag);
            request.startTime = context.getTimeSource().getCurrentTime();
            res = request;
        } else
            res = this;

        setRecursive(true);
        container.inCall = false;

        return res;
    }

    @Override
    public void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return;

        container.inCall = true;
        setRecursive(false);

        if (param instanceof JmsConsumerRawRequest) {
            JmsConsumerRawRequest jmsRequest = (JmsConsumerRawRequest) param;
            jmsRequest.endTime = context.getTimeSource().getCurrentTime();
            endRequest(container, null);
        }

        container.scopes.activateAll();

        container.inCall = false;
    }

    @Override
    public void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        onReturnExit(index, version, param, instance, null);
    }

    @Override
    public Object onCallEnter(int index, int version, Object instance, Object callee, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return null;

        container.inCall = true;

        if (isRecursive()) {
            IRequest request = getRequest();
            if (request != null) {
                JmsConsumerRawRequest jmsRequest = (JmsConsumerRawRequest) request.getRawRequest();
                jmsRequest.endTime = context.getTimeSource().getCurrentTime();
            }

            endRequest(container, null);
        }

        setRecursive(false);

        container.scopes.activateAll();
        container.inCall = false;
        return this;
    }

    @Override
    public void onCallReturnExit(int index, int version, Object param, Object instance, Object callee, Object retVal) {
        if (retVal == null)
            return;

        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || isRecursive())
            return;

        container.inCall = true;
        container.scopes.deactivateAll();

        JmsConsumerRawRequest request = createRequest(container, retVal);
        if (request != null) {
            beginRequest(container, request, request.tag);
            request.startTime = context.getTimeSource().getCurrentTime();
        }

        setRecursive(true);
        container.inCall = false;
    }

    @Override
    protected EntryPointProbeCollector doCreateCollector(int index, String combineId, ICallPath callPath,
                                                         String name, StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata, boolean primary, boolean leaf) {
        return new JmsConsumerProbeCollector(index, this, name, combineId,
                callPath, root, parent, metadata, primary, leaf);
    }

    @Override
    protected IRequest mapScope(Object rawRequest) {
        JmsConsumerRawRequest request = (JmsConsumerRawRequest) rawRequest;
        return new Request("jms:" + request.destinationType + ":" + request.destinationName, null);
    }

    private JmsConsumerRawRequest createRequest(Container container, Object request) {
        IJmsConsumerBridge bridge = bridgeHolder.get(request);
        String tagString = bridge.getTag(request);
        TraceTag tag = null;
        if (tagString != null)
            tag = TraceTag.fromString(tagString);

        String destinationName = bridge.getDestinationName(request);
        String destinationType = bridge.getDestinationType(request);

        int size = 0;
        if (configuration.getReceiveBytesCounter().isEnabled())
            size = bridge.getSize(request);

        return new JmsConsumerRawRequest(request, destinationName, destinationType, size, tag);
    }
}
