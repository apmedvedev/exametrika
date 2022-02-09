/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.metrics.jvm.config.UdpProbeConfiguration;
import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.metrics.jvm.boot.UdpProbeInterceptor;
import com.exametrika.impl.profiler.probes.ExitPointProbe;
import com.exametrika.impl.profiler.probes.ExitPointProbeCalibrateInfo;
import com.exametrika.impl.profiler.probes.ExitPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.metrics.jvm.IUdpRawRequest;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.Request;


/**
 * The {@link UdpProbe} is a UDP socket probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UdpProbe extends ExitPointProbe {
    private final UdpProbeConfiguration configuration;

    public static class UdpRawRequest extends Request implements IUdpRawRequest {
        private long size;
        private final boolean receive;
        private long startTime;
        private long delta;

        public UdpRawRequest(InetSocketAddress address, int size, boolean receive) {
            super(address.getAddress().getCanonicalHostName() + ":" + address.getPort(), null);
            this.size = size;
            this.receive = receive;
        }

        public long getSize() {
            return size;
        }

        @Override
        public String getHostPort() {
            return getName();
        }

        @Override
        public boolean isReceive() {
            return receive;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getDelta() {
            return delta;
        }
    }

    public UdpProbe(UdpProbeConfiguration configuration, IProbeContext context, int index) {
        super(configuration, context, index, "udpProbe");

        this.configuration = configuration;
    }

    @Override
    public void start() {
        UdpProbeInterceptor.interceptor = this;
    }

    @Override
    public void stop() {
        UdpProbeInterceptor.interceptor = null;
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || container.top == null || isRecursive())
            return null;

        container.inCall = true;

        Object request = createRequest(index, instance, params);
        if (request instanceof UdpRawRequest) {
            beginRequest(container, request);
            ((UdpRawRequest) request).startTime = getStartTime();
        }

        if (request != null)
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

        updateRequest(container, index, param, instance, retVal);

        container.inCall = false;
    }

    @Override
    public void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        onReturnExit(index, version, param, instance, 0);
    }

    @Override
    protected ExitPointProbeCollector doCreateCollector(int index, String name, UUID stackId, ICallPath callPath,
                                                        StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata,
                                                        ExitPointProbeCalibrateInfo calibrateInfo, boolean leaf) {
        return new UdpProbeCollector(configuration, index, name, stackId, callPath, root, parent, metadata, calibrateInfo, leaf);
    }

    @Override
    protected IRequest mapRequest(IScope scope, Object rawRequest) {
        return (IRequest) rawRequest;
    }

    @Override
    protected Object createCalibratingRequest() {
        return new UdpRawRequest(new InetSocketAddress("localhost", 7777), 0, false);
    }

    private Object createRequest(int index, Object instance, Object[] params) {
        IJoinPoint joinPoint = context.getJoinPointProvider().findJoinPoint(index, -1);
        if (joinPoint == null)
            return null;

        DatagramSocket socket;
        if (instance instanceof DatagramSocket)
            socket = (DatagramSocket) instance;
        else
            socket = ((DatagramChannel) instance).socket();

        InetSocketAddress address = null;
        if (socket.isConnected())
            address = (InetSocketAddress) socket.getRemoteSocketAddress();

        if (joinPoint.getMethodName().startsWith("receive")) {
            long startTime = getStartTime();

            if (params[0] instanceof DatagramPacket)
                return new ReceiveRequest((DatagramPacket) params[0], startTime);
            else if (params[0] instanceof ByteBuffer) {
                ByteBuffer buffer = (ByteBuffer) params[0];
                return new ReceiveRequest(buffer, startTime, buffer.position());
            }
        }

        boolean receive = joinPoint.getMethodName().startsWith("read");

        int size = 0;
        if (instance instanceof DatagramSocket) {
            DatagramPacket packet = (DatagramPacket) params[0];
            size = packet.getLength();
            if (address == null)
                address = new InetSocketAddress(packet.getAddress(), packet.getPort());
        } else if (!(instance instanceof DatagramChannel))
            return null;

        return new UdpRawRequest(address, size, receive);
    }

    private void updateRequest(Container container, int index, Object rawRequest, Object instance, Object retVal) {
        UdpRawRequest request;
        if (rawRequest instanceof ReceiveRequest) {
            ReceiveRequest receiveRequest = (ReceiveRequest) rawRequest;

            InetSocketAddress address;
            int size;
            if (instance instanceof DatagramSocket) {
                DatagramPacket packet = receiveRequest.packet;
                if (packet == null || packet.getAddress() == null)
                    return;

                size = packet.getLength();
                address = new InetSocketAddress(packet.getAddress(), packet.getPort());
            } else if (instance instanceof DatagramChannel) {
                if (retVal == null || receiveRequest.buffer == null)
                    return;

                address = (InetSocketAddress) retVal;
                size = (int) (receiveRequest.buffer.position() - receiveRequest.startPosition);
            } else
                return;

            request = new UdpRawRequest(address, size, true);
            request.startTime = receiveRequest.startTime;
            beginRequest(container, request);
        } else {
            request = (UdpRawRequest) rawRequest;
            if (instance instanceof DatagramChannel) {
                request.size = ((Number) retVal).longValue();
                if (request.size == -1)
                    request.size = 0;
            }
        }

        request.delta = getTimeDelta(request.startTime);

        long[] counters = container.counters;
        counters[AppStackCounterType.IO_COUNT.ordinal()]++;
        counters[AppStackCounterType.IO_BYTES.ordinal()] += request.size;
        counters[AppStackCounterType.IO_TIME.ordinal()] += request.delta;
        counters[AppStackCounterType.NET_COUNT.ordinal()]++;
        counters[AppStackCounterType.NET_BYTES.ordinal()] += request.size;
        counters[AppStackCounterType.NET_TIME.ordinal()] += request.delta;
        if (request.receive) {
            counters[AppStackCounterType.NET_RECEIVE_COUNT.ordinal()]++;
            counters[AppStackCounterType.NET_RECEIVE_BYTES.ordinal()] += request.size;
            counters[AppStackCounterType.NET_RECEIVE_TIME.ordinal()] += request.delta;
        } else {
            counters[AppStackCounterType.NET_SEND_COUNT.ordinal()]++;
            counters[AppStackCounterType.NET_SEND_BYTES.ordinal()] += request.size;
            counters[AppStackCounterType.NET_SEND_TIME.ordinal()] += request.delta;
        }

        endRequest(container, null, request.delta, 0);
    }

    public static class ReceiveRequest {
        private final long startTime;
        private final DatagramPacket packet;
        private final ByteBuffer buffer;
        private final long startPosition;

        public ReceiveRequest(DatagramPacket packet, long startTime) {
            this.packet = packet;
            this.startTime = startTime;
            this.buffer = null;
            this.startPosition = 0;
        }

        public ReceiveRequest(ByteBuffer buffer, long startTime, long startPosition) {
            this.buffer = buffer;
            this.startTime = startTime;
            this.startPosition = startPosition;
            this.packet = null;
        }
    }
}
