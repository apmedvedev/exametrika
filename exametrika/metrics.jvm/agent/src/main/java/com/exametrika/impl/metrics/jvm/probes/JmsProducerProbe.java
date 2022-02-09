/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.metrics.jvm.config.JmsProducerProbeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.metrics.jvm.boot.JmsProducerProbeInterceptor;
import com.exametrika.impl.profiler.bridge.BridgeHolder;
import com.exametrika.impl.profiler.probes.ExitPointProbe;
import com.exametrika.impl.profiler.probes.ExitPointProbeCalibrateInfo;
import com.exametrika.impl.profiler.probes.ExitPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.metrics.jvm.IJmsProducerRawRequest;
import com.exametrika.spi.metrics.jvm.boot.IJmsProducerBridge;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.Request;
import com.exametrika.spi.profiler.TraceTag;


/**
 * The {@link JmsProducerProbe} is a JMS producer probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JmsProducerProbe extends ExitPointProbe {
    private final JmsProducerProbeConfiguration configuration;
    private final BridgeHolder<IJmsProducerBridge> bridgeHolder;

    public class JmsProducerRawRequest extends Request implements IJmsProducerRawRequest {
        private final String destinationType;
        private final JsonObject metadata;
        private final int size;

        public JmsProducerRawRequest(String destinationName, String destinationType, Object request, JsonObject metadata, int size) {
            super(destinationName, request);

            this.destinationType = destinationType;
            this.metadata = metadata;
            this.size = size;
        }

        @Override
        public String getDestinationType() {
            return destinationType;
        }

        @Override
        public String getDestinationName() {
            return getName();
        }

        public int getSize() {
            return size;
        }

        @Override
        public JsonObject getMetadata() {
            return metadata;
        }
    }

    public JmsProducerProbe(JmsProducerProbeConfiguration configuration, IProbeContext context, int index) {
        super(configuration, context, index, "jmsProducerProbe");

        this.configuration = configuration;
        this.bridgeHolder = new BridgeHolder<IJmsProducerBridge>(IJmsProducerBridge.class,
                Collections.asSet("javax.jms.Message", "javax.jms.MessageProducer", "javax.jms.JMSProducer"),
                Integer.MAX_VALUE, context.getTimeService());
    }

    @Override
    public void start() {
        JmsProducerProbeInterceptor.interceptor = this;
    }

    @Override
    public void stop() {
        JmsProducerProbeInterceptor.interceptor = null;
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || container.top == null || isRecursive())
            return null;

        container.inCall = true;

        IJmsProducerBridge bridge = bridgeHolder.get(instance);

        Object request;
        if (!updateMessageSize(bridge, instance, params)) {
            request = createRequest(bridge, instance, params);
            beginRequest(container, request);
        } else
            request = this;

        setRecursive(true);

        container.inCall = false;

        return request;
    }

    @Override
    public void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return;

        container.inCall = true;
        setRecursive(false);

        if (param instanceof JmsProducerRawRequest)
            endRequest(container, null, 0, 0);

        container.inCall = false;
    }

    @Override
    public void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        onReturnExit(index, version, param, instance, null);
    }

    @Override
    protected ExitPointProbeCollector doCreateCollector(int index, String name, UUID stackId, ICallPath callPath,
                                                        StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata,
                                                        ExitPointProbeCalibrateInfo calibrateInfo, boolean leaf) {
        return new JmsProducerProbeCollector(configuration, index, name, stackId, callPath, root, parent, metadata,
                calibrateInfo, leaf);
    }

    @Override
    protected IRequest mapRequest(IScope scope, Object rawRequest) {
        return (IRequest) rawRequest;
    }

    @Override
    protected void writeTag(Object request, TraceTag tag) {
        Object jmsRequest = ((JmsProducerRawRequest) request).getRawRequest();
        if (jmsRequest instanceof JmsProducerRawRequest)
            return;

        IJmsProducerBridge bridge = bridgeHolder.get(jmsRequest);
        if (bridge.isMessage(jmsRequest))
            bridge.setMessageTag(jmsRequest, tag.toString());
        else if (bridge.isJmsProducer(jmsRequest))
            bridge.setProducerTag(jmsRequest, tag.toString());
        else
            Assert.error();
    }

    @Override
    protected Object createCalibratingRequest() {
        return new JmsProducerRawRequest("calibrate", "query", null, null, 0);
    }

    private boolean updateMessageSize(IJmsProducerBridge bridge, Object instance, Object[] params) {
        if (bridge.isBytesOrStreamMessage(instance)) {
            if (configuration.getBytesCounter().isEnabled()) {
                int size = 0;
                if (params.length == 1)
                    size = getPrimitiveSize(params[0]);
                else if (params.length == 3)
                    size = ((Integer) params[2]);

                bridge.updateMessageSize(instance, size);
            }

            return true;
        }

        return false;
    }

    private JmsProducerRawRequest createRequest(IJmsProducerBridge bridge, Object instance, Object[] params) {
        String destinationName;
        String destinationType;
        int size = 0;
        Object rawRequest;
        if (bridge.isJmsProducer(instance)) {
            destinationName = bridge.getDestinationName(params[0]);
            destinationType = bridge.getDestinationType(params[0]);
            rawRequest = instance;
            if (configuration.getBytesCounter().isEnabled()) {
                size = getMessageSize(bridge, params[1]);
                bridge.setProducerSize(instance, size);
            }
        } else if (bridge.isMessage(params[0])) {
            Object destination = bridge.getDestination(instance);
            destinationName = bridge.getDestinationName(destination);
            destinationType = bridge.getDestinationType(destination);
            rawRequest = params[0];
            if (configuration.getBytesCounter().isEnabled()) {
                size = getMessageSize(bridge, params[0]);
                bridge.setMessageSize(params[0], size);
            }
        } else {
            destinationName = bridge.getDestinationName(params[0]);
            destinationType = bridge.getDestinationType(params[0]);
            rawRequest = params[1];
            if (configuration.getBytesCounter().isEnabled()) {
                size = getMessageSize(bridge, params[1]);
                bridge.setMessageSize(params[1], size);
            }
        }

        JsonObject metadata = Json.object()
                .put("destination", destinationName)
                .put("destinationType", destinationType)
                .toObject();

        return new JmsProducerRawRequest(destinationName, destinationType, rawRequest, metadata, size);
    }

    private int getMessageSize(IJmsProducerBridge bridge, Object message) {
        if (message instanceof byte[])
            return ((byte[]) message).length;
        else if (message instanceof String)
            return 2 * ((String) message).length();
        else if (message instanceof Map) {
            int size = 0;
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) message).entrySet())
                size += 2 * entry.getKey().length() + getPrimitiveSize(entry.getValue());

            return size;
        } else if (bridge.isBytesOrStreamMessage(message))
            return bridge.getSize(message);
        else if (bridge.isTextMessage(message))
            return getMessageSize(bridge, bridge.getText(message));
        else if (bridge.isMapMessage(message))
            return bridge.getMapMessageSize(message);
        else if (bridge.isObjectMessage(message))
            return getMessageSize(bridge, bridge.getObject(message));
        else if (message instanceof Serializable) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try {
                ObjectOutputStream stream = new ObjectOutputStream(byteStream);
                stream.writeObject(message);
                IOs.close(stream);

                return byteStream.size();
            } catch (IOException e) {
                return 0;
            }
        } else
            return 0;
    }

    private int getPrimitiveSize(Object value) {
        if (value instanceof byte[])
            return ((byte[]) value).length;
        else if (value instanceof String)
            return 2 * ((String) value).length();
        else if (value instanceof Boolean || value instanceof Byte)
            return 1;
        else if (value instanceof Short || value instanceof Character)
            return 2;
        else if (value instanceof Integer || value instanceof Float)
            return 4;
        else if (value instanceof Long || value instanceof Double)
            return 8;
        else
            return 0;
    }
}
