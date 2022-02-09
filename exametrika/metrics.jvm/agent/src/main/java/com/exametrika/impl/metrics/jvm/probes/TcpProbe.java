/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketImpl;
import java.nio.channels.SocketChannel;
import java.util.UUID;

import sun.nio.ch.ChannelInputStream;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.metrics.jvm.config.TcpProbeConfiguration;
import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Fields;
import com.exametrika.common.utils.Fields.IField;
import com.exametrika.impl.metrics.jvm.boot.TcpProbeInterceptor;
import com.exametrika.impl.profiler.probes.ExitPointProbe;
import com.exametrika.impl.profiler.probes.ExitPointProbeCalibrateInfo;
import com.exametrika.impl.profiler.probes.ExitPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.metrics.jvm.ITcpRawRequest;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.Request;


/**
 * The {@link TcpProbe} is a TCP socket probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TcpProbe extends ExitPointProbe {
    private static final IField socketInputStreamImplField = Fields.get("java.net.SocketInputStream", "impl");
    private static final IField socketOutputStreamImplField = Fields.get("java.net.SocketOutputStream", "impl");
    private static final IField socketImplAddressField = Fields.get("java.net.SocketImpl", "address");
    private static final IField socketImplPortField = Fields.get("java.net.SocketImpl", "port");
    private static final IField socketAdaptorChannelField = Fields.get("sun.nio.ch.SocketAdaptor$SocketInputStream", "ch");
    private final TcpProbeConfiguration configuration;

    public static class TcpRawRequest extends Request implements ITcpRawRequest {
        private long size;
        private final boolean connect;
        private final boolean receive;
        private long startTime;
        private long delta;

        public TcpRawRequest(InetSocketAddress address, int size, boolean receive, boolean connect) {
            super(getName(address), null);
            this.size = size;
            this.receive = receive;
            this.connect = connect;
        }

        public long getSize() {
            return size;
        }

        public boolean isConnect() {
            return connect;
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

        private static String getName(InetSocketAddress address) {
            if (address != null) {
                if (address.getAddress() != null) {
                    if (address.getAddress().getCanonicalHostName() != null)
                        return address.getAddress().getCanonicalHostName() + ":" + address.getPort();
                    else if (address.getAddress().getHostName() != null)
                        return address.getAddress().getHostName() + ":" + address.getPort();
                    else
                        return address.getAddress().getHostAddress() + ":" + address.getPort();
                } else
                    return "<unknown>:" + address.getPort();
            } else
                return "<unknown>";
        }
    }

    public TcpProbe(TcpProbeConfiguration configuration, IProbeContext context, int index) {
        super(configuration, context, index, "tcpProbe");

        this.configuration = configuration;
    }

    @Override
    public void start() {
        TcpProbeInterceptor.interceptor = this;
    }

    @Override
    public void stop() {
        TcpProbeInterceptor.interceptor = null;
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || container.top == null || isRecursive())
            return null;

        container.inCall = true;

        TcpRawRequest request = createRequest(index, instance, params);
        if (request != null) {
            beginRequest(container, request);
            request.startTime = getStartTime();
            setRecursive(true);
        }

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

        TcpRawRequest request = (TcpRawRequest) param;
        request.delta = getTimeDelta(request.startTime);
        updateRequest(container, request, instance, retVal);

        endRequest(container, null, request.delta, 0);

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
        return new TcpProbeCollector(configuration, index, name, stackId, callPath, root, parent, metadata, calibrateInfo, leaf);
    }

    @Override
    protected IRequest mapRequest(IScope scope, Object rawRequest) {
        return (IRequest) rawRequest;
    }

    @Override
    protected Object createCalibratingRequest() {
        return new TcpRawRequest(new InetSocketAddress("localhost", 7777), 0, false, false);
    }

    private TcpRawRequest createRequest(int index, Object instance, Object[] params) {
        IJoinPoint joinPoint = context.getJoinPointProvider().findJoinPoint(index, -1);
        if (joinPoint == null)
            return null;

        boolean receive = joinPoint.getMethodName().startsWith("read");
        boolean connect = joinPoint.getMethodName().startsWith("connect");

        InetSocketAddress address;
        int size = 0;
        if (connect)
            address = (InetSocketAddress) params[0];
        else {
            address = getAddress(instance);
            if (instance instanceof FileOutputStream)
                size = ((Number) params[2]).intValue();
        }

        return new TcpRawRequest(address, size, receive, connect);
    }

    private void updateRequest(Container container, TcpRawRequest request, Object instance, Object retVal) {
        if (!request.connect && retVal != null) {
            request.size = ((Number) retVal).longValue();
            if (request.size == -1)
                request.size = 0;
        }

        long[] counters = container.counters;
        counters[AppStackCounterType.IO_COUNT.ordinal()]++;
        counters[AppStackCounterType.IO_BYTES.ordinal()] += request.size;
        counters[AppStackCounterType.IO_TIME.ordinal()] += request.delta;
        counters[AppStackCounterType.NET_COUNT.ordinal()]++;
        counters[AppStackCounterType.NET_BYTES.ordinal()] += request.size;
        counters[AppStackCounterType.NET_TIME.ordinal()] += request.delta;
        if (request.connect) {
            counters[AppStackCounterType.NET_CONNECT_COUNT.ordinal()]++;
            counters[AppStackCounterType.NET_CONNECT_TIME.ordinal()] += request.delta;
        } else if (request.receive) {
            counters[AppStackCounterType.NET_RECEIVE_COUNT.ordinal()]++;
            counters[AppStackCounterType.NET_RECEIVE_BYTES.ordinal()] += request.size;
            counters[AppStackCounterType.NET_RECEIVE_TIME.ordinal()] += request.delta;
        } else {
            counters[AppStackCounterType.NET_SEND_COUNT.ordinal()]++;
            counters[AppStackCounterType.NET_SEND_BYTES.ordinal()] += request.size;
            counters[AppStackCounterType.NET_SEND_TIME.ordinal()] += request.delta;
        }
    }

    private static InetSocketAddress getAddress(Object instance) {
        if (instance instanceof SocketChannel) {
            Socket socket = ((SocketChannel) instance).socket();
            return (InetSocketAddress) socket.getRemoteSocketAddress();
        }

        if (instance instanceof FileInputStream || instance instanceof FileOutputStream) {
            IField field;
            if (instance instanceof FileInputStream)
                field = socketInputStreamImplField;
            else
                field = socketOutputStreamImplField;

            if (field == null)
                return null;

            SocketImpl impl = (SocketImpl) field.getObject(instance);
            InetAddress address;
            if (socketImplAddressField != null && impl != null)
                address = (InetAddress) socketImplAddressField.getObject(impl);
            else
                return null;

            int port;
            if (socketImplPortField != null)
                port = socketImplPortField.getInt(impl);
            else
                return null;

            return new InetSocketAddress(address, port);
        } else if (instance instanceof ChannelInputStream && socketAdaptorChannelField != null) {
            Socket socket = ((SocketChannel) socketAdaptorChannelField.getObject(instance)).socket();
            return (InetSocketAddress) socket.getRemoteSocketAddress();
        }

        return null;
    }
}
