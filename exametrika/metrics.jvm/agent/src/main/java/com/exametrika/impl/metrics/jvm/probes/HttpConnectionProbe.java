/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.metrics.jvm.config.HttpConnectionProbeConfiguration;
import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Fields;
import com.exametrika.common.utils.Fields.IField;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.metrics.jvm.boot.HttpConnectionProbeInterceptor;
import com.exametrika.impl.metrics.jvm.boot.IHttpConnectionProbeInterceptor;
import com.exametrika.impl.profiler.probes.ExitPointProbe;
import com.exametrika.impl.profiler.probes.ExitPointProbeCalibrateInfo;
import com.exametrika.impl.profiler.probes.ExitPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.metrics.jvm.IHttpConnectionRawRequest;
import com.exametrika.spi.metrics.jvm.boot.IHttpServletBridge;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.Probes;
import com.exametrika.spi.profiler.TraceTag;


/**
 * The {@link HttpConnectionProbe} is a HTTP connection probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HttpConnectionProbe extends ExitPointProbe implements IHttpConnectionProbeInterceptor {
    private static final IField connectionConnectedField = Fields.get(java.net.URLConnection.class, "connected");
    private static final IField connectionConnectingField = Fields.get("sun.net.www.protocol.http.HttpURLConnection", "connecting");
    private static final IField streamingWrittenField = Fields.get("sun.net.www.protocol.http.HttpURLConnection$StreamingOutputStream",
            "written");
    private static final IField inputStreamField = Fields.get("sun.net.www.protocol.http.HttpURLConnection",
            "inputStream");
    private static final IField strOutputStreamField = Fields.get("sun.net.www.protocol.http.HttpURLConnection",
            "strOutputStream");
    private static final IField posterField = Fields.get("sun.net.www.protocol.http.HttpURLConnection",
            "poster");
    private static final IField errorStreamField = Fields.get("sun.net.www.protocol.http.HttpURLConnection",
            "errorStream");
    private final HttpConnectionProbeConfiguration configuration;

    public static class HttpConnectionRawRequest implements IHttpConnectionRawRequest {
        private final HttpURLConnection connection;
        private long receiveSize;
        private final long sendSize;
        private final boolean connect;
        private boolean receiveSizeSet;
        private long startTime;
        private long delta;
        private long requestTime;

        public HttpConnectionRawRequest(HttpURLConnection connection, long sendSize, boolean connect) {
            this.connection = connection;
            this.sendSize = sendSize;
            this.connect = connect;
        }

        @Override
        public String getUrlWithQueryString() {
            if (connection != null)
                return connection.getURL().toString();
            else
                return null;
        }

        @Override
        public String getUrl() {
            if (connection != null) {
                String url = connection.getURL().toString();
                int pos = url.lastIndexOf('?');
                if (pos != -1)
                    return url.substring(0, pos);
                else
                    return url;
            } else
                return null;
        }

        @Override
        public String getHostPort() {
            if (connection != null) {
                URL url = connection.getURL();
                return url.getProtocol() + "://" + url.getHost() + (url.getPort() != -1 ? (":" + url.getPort()) : "");
            } else
                return null;
        }

        @Override
        public HttpURLConnection getConnection() {
            return connection;
        }

        public boolean isReceiveSizeSet() {
            return receiveSizeSet;
        }

        public long getReceiveSize() {
            return receiveSize;
        }

        public long getSendSize() {
            return sendSize;
        }

        public boolean isConnect() {
            return connect;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getDelta() {
            return delta;
        }
    }

    public HttpConnectionProbe(HttpConnectionProbeConfiguration configuration, IProbeContext context, int index) {
        super(configuration, context, index, "httpConnectionProbe");

        this.configuration = configuration;
    }

    @Override
    public synchronized void start() {
        HttpConnectionProbeInterceptor.interceptor = this;
    }

    @Override
    public synchronized void stop() {
        HttpConnectionProbeInterceptor.interceptor = null;
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || container.top == null || isRecursive())
            return null;

        container.inCall = true;

        HttpConnectionRawRequest request = createRequest(container, index, instance);
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

        HttpConnectionRawRequest httpRequest = (HttpConnectionRawRequest) param;
        httpRequest.delta = getTimeDelta(httpRequest.startTime);
        updateRequest(container, httpRequest, retVal);

        endRequest(container, null, httpRequest.delta, httpRequest.requestTime);

        container.inCall = false;
    }

    @Override
    public InputStream onReturnExit(Object param, Object retVal) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return (InputStream) retVal;

        container.inCall = true;
        setRecursive(false);

        HttpConnectionRawRequest httpRequest = (HttpConnectionRawRequest) param;
        httpRequest.delta = getTimeDelta(httpRequest.startTime);
        retVal = updateRequest(container, httpRequest, retVal);

        endRequest(container, null, httpRequest.delta, httpRequest.requestTime);

        container.inCall = false;

        return (InputStream) retVal;
    }

    @Override
    public void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        onReturnExit(param, null);
    }

    @Override
    protected ExitPointProbeCollector doCreateCollector(int index, String name, UUID stackId, ICallPath callPath, StackProbeRootCollector root,
                                                        StackProbeCollector parent, JsonObject metadata, ExitPointProbeCalibrateInfo calibrateInfo, boolean leaf) {
        return new HttpConnectionProbeCollector(configuration, index, name, stackId, callPath, root, parent, metadata, calibrateInfo, leaf);
    }

    @Override
    protected void writeTag(Object request, TraceTag tag) {
        HttpURLConnection connection = ((HttpConnectionRawRequest) request).connection;
        if (connection != null && !connectionConnectedField.getBoolean(connection) &&
                (connectionConnectingField == null || !connectionConnectingField.getBoolean(connection)))
            connection.setRequestProperty(IHttpServletBridge.EXA_TRACE_TAG, tag.toString());
    }

    private HttpConnectionRawRequest createRequest(Container container, int index, Object instance) {
        HttpURLConnection connection = (HttpURLConnection) instance;

        if (!Probes.isInstanceOf(connection, "sun.net.www.protocol.http.HttpURLConnection") ||
                inputStreamField.getObject(connection) != null)
            return null;

        long sendSize = 0;

        IJoinPoint joinPoint = context.getJoinPointProvider().findJoinPoint(index, -1);
        if (joinPoint == null)
            return null;

        if (joinPoint.getMethodName().startsWith("connect") || joinPoint.getMethodName().startsWith("getOutputStream")) {
            if (connection != null && !connectionConnectedField.getBoolean(connection) &&
                    (connectionConnectingField == null || !connectionConnectingField.getBoolean(connection)))
                writeTag(container, new HttpConnectionRawRequest(connection, 0, true));

            return null;
        } else if (connection.getDoOutput()) {
            OutputStream output = posterField.getObject(connection);
            if (output instanceof ByteArrayOutputStream)
                sendSize = ((ByteArrayOutputStream) output).size();
            else {
                output = strOutputStreamField.getObject(connection);
                if (Probes.isInstanceOf(output, "sun.net.www.protocol.http.HttpURLConnection$StreamingOutputStream") &&
                        streamingWrittenField != null)
                    sendSize = streamingWrittenField.getLong(output);
            }
        }

        return new HttpConnectionRawRequest((HttpURLConnection) instance, sendSize, false);
    }

    @Override
    protected Object createCalibratingRequest() {
        return new HttpConnectionRawRequest(null, 0, false);
    }

    private Object updateRequest(Container container, HttpConnectionRawRequest request, Object retVal) {
        if (!request.connect) {
            HttpURLConnection connection = request.connection;
            if (connection == null)
                return retVal;

            if (configuration.getErrorsLog() != null) {
                try {
                    int status = connection.getResponseCode();
                    if (status >= 400) {
                        int size = configuration.getErrorsLog().getMaxMessageSize() * 2;
                        InputStream stream = connection.getErrorStream();
                        String message;
                        if (stream != null && errorStreamField != null &&
                                Probes.isInstanceOf(connection, "sun.net.www.protocol.http.HttpURLConnection")) {
                            ByteOutputStream out = new ByteOutputStream();
                            IOs.copy(stream, out);
                            IOs.close(stream);

                            message = new String(out.getBuffer(), 0, Math.min(size, out.getLength()));
                            ByteArrayInputStream in = new ByteArrayInputStream(out.getBuffer(), 0, out.getLength());

                            errorStreamField.setObject(connection, in);
                        } else
                            message = connection.getResponseMessage();

                        logError(container, status, message);
                    }
                } catch (IOException e) {
                }
            }

            int length = connection.getContentLength();
            if (length != -1) {
                request.receiveSizeSet = true;
                request.receiveSize = length;
            } else if (retVal instanceof InputStream) {
                HttpConnectionProbeCollector collector = getCollector();
                if (collector != null)
                    retVal = new ProbeInputStream((InputStream) retVal, collector);
            }

            String requestTimeHeader = connection.getHeaderField(IHttpServletBridge.EXA_REQUEST_TIME);
            if (requestTimeHeader != null)
                request.requestTime = Long.parseLong(requestTimeHeader);
        }

        return retVal;
    }

    private void logError(Container container, int status, String message) {
        long[] counters = container.counters;
        counters[AppStackCounterType.ERRORS_COUNT.ordinal()]++;

        HttpConnectionProbeCollector collector = getCollector();
        IRequest request = getRequest();
        if (collector == null || request == null)
            return;

        collector.logError(request, status, message);
    }

    private static class ProbeInputStream extends FilterInputStream {
        private final HttpConnectionProbeCollector collector;
        private long receiveSize;
        private long markReceiveSize;

        private ProbeInputStream(InputStream in, HttpConnectionProbeCollector collector) {
            super(in);

            this.collector = collector;
        }

        @Override
        public int read() throws IOException {
            int n = super.read();
            if (n != -1)
                receiveSize++;

            return n;
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            int n = super.read(b, off, len);
            if (n != -1)
                receiveSize += n;

            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = super.skip(n);
            receiveSize += skipped;
            return skipped;
        }

        @Override
        public void close() throws IOException {
            collector.addReceiveSize(receiveSize);
            super.close();
        }

        @Override
        public void mark(int readlimit) {
            markReceiveSize = receiveSize;
            super.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            receiveSize = markReceiveSize;
            super.reset();
        }
    }
}
