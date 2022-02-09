/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.metrics.jvm.config.HttpServletProbeConfiguration;
import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.impl.metrics.jvm.boot.HttpServletProbeInterceptor;
import com.exametrika.impl.metrics.jvm.boot.IHttpServletProbeInterceptor;
import com.exametrika.impl.profiler.bridge.BridgeHolder;
import com.exametrika.impl.profiler.probes.EntryPointProbe;
import com.exametrika.impl.profiler.probes.EntryPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.metrics.jvm.IHttpServletRawRequest;
import com.exametrika.spi.metrics.jvm.boot.IHttpServletBridge;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.Request;
import com.exametrika.spi.profiler.TraceTag;


/**
 * The {@link HttpServletProbe} is a HTTP servlet probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HttpServletProbe extends EntryPointProbe implements IHttpServletProbeInterceptor {
    private final BridgeHolder<IHttpServletBridge> bridgeHolder;

    public static class HttpServletRawRequest implements IHttpServletRawRequest {
        private final Object servlet;
        private final Object request;
        private final Object response;
        private final String contextName;
        private final String url;
        private final String queryString;
        private long receiveSize;
        private boolean receiveSizeSet;
        private long sendSize;
        private boolean hasParams;
        private boolean sendSizeSet;
        private long startTime;
        private long endTime;
        private boolean inCall;
        private final int variant;
        private boolean requestTimeSet;

        public HttpServletRawRequest(Object servlet, Object request, Object response, String contextName, String url, String queryString, int variant) {
            this.servlet = servlet;
            this.request = request;
            this.response = response;
            this.contextName = contextName;
            this.url = url;
            this.queryString = queryString;
            this.variant = variant;
        }

        @Override
        public String getContextName() {
            return contextName;
        }

        @Override
        public String getUrl() {
            return url;
        }

        public String getAppUrl() {
            return contextName + ":" + url;
        }

        @Override
        public String getUrlWithQueryString() {
            return (queryString != null) ? (url + '?' + queryString) : url;
        }

        public String getAppUrlWithQueryString() {
            return contextName + ":" + getUrlWithQueryString();
        }

        @Override
        public String getQueryString() {
            return queryString;
        }

        @Override
        public Object getServlet() {
            return servlet;
        }

        @Override
        public Object getRequest() {
            return request;
        }

        @Override
        public Object getResponse() {
            return response;
        }

        public long getReceiveSize() {
            return receiveSize;
        }

        public long getSendSize() {
            return sendSize;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public int getVariant() {
            return variant;
        }
    }

    public HttpServletProbe(HttpServletProbeConfiguration configuration, IProbeContext context, int index) {
        super(configuration, context, index);

        this.bridgeHolder = new BridgeHolder<IHttpServletBridge>(IHttpServletBridge.class,
                Collections.asSet("javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse"),
                Integer.MAX_VALUE, context.getTimeService());
    }

    @Override
    public Object allocate() {
        return new HttpEntryPointInfo();
    }

    @Override
    public synchronized void start() {
        HttpServletProbeInterceptor.interceptor = this;
    }

    @Override
    public synchronized void stop() {
        HttpServletProbeInterceptor.interceptor = null;
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || isRecursive2())
            return null;

        IJoinPoint joinPoint = context.getJoinPointProvider().findJoinPoint(index, version);
        if (joinPoint == null)
            return null;

        container.inCall = true;

        Object res = this;

        if (joinPoint.getMethodName().startsWith("sendError") || joinPoint.getMethodName().startsWith("setStatus")) {
            if (((Integer) params[0]) >= 400)
                logError(container, (Integer) params[0], params.length > 1 ? (String) params[1] : null);
        } else {
            IRequest request = getRequest();
            if (request != null) {
                HttpServletRawRequest httpRequest = request.getRawRequest();
                if (joinPoint.getMethodName().startsWith("setContentLength")) {
                    httpRequest.sendSize = (Integer) params[0];
                    httpRequest.sendSizeSet = true;
                } else if (!httpRequest.requestTimeSet &&
                        (joinPoint.getMethodName().startsWith("getWriter") || joinPoint.getMethodName().startsWith("getOutputStream"))) {
                    IHttpServletBridge bridge = bridgeHolder.get(instance);

                    long endTime = context.getTimeSource().getCurrentTime();
                    bridge.setRequestTime(instance, endTime - httpRequest.startTime);
                    httpRequest.requestTimeSet = true;
                } else if (joinPoint.getMethodSignature().startsWith("reset()")) {
                    httpRequest.sendSize = 0;
                    httpRequest.sendSizeSet = false;
                } else if (joinPoint.getMethodSignature().startsWith("resetBuffer()")) {
                    if (!httpRequest.sendSizeSet)
                        httpRequest.sendSize = 0;
                } else if (joinPoint.getMethodName().startsWith("read")) {
                    if (!httpRequest.inCall && !httpRequest.receiveSizeSet) {
                        httpRequest.hasParams = params.length > 0;
                        res = httpRequest;
                    }
                } else if (joinPoint.getMethodName().startsWith("write")) {
                    if (!httpRequest.inCall && !httpRequest.sendSizeSet) {
                        if (params.length == 1)
                            httpRequest.sendSize++;
                        else
                            httpRequest.sendSize += (Integer) params[2];
                    }
                }
            }
        }

        setRecursive2(true);
        container.inCall = false;

        return res;
    }

    @Override
    public void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return;

        container.inCall = true;
        setRecursive2(false);

        IJoinPoint joinPoint = context.getJoinPointProvider().findJoinPoint(index, version);
        if (joinPoint != null && param instanceof HttpServletRawRequest) {
            HttpServletRawRequest httpRequest = (HttpServletRawRequest) param;

            if (!httpRequest.inCall && !httpRequest.receiveSizeSet && joinPoint.getMethodName().startsWith("read")) {
                if (httpRequest.hasParams)
                    httpRequest.receiveSize += (Integer) retVal;
                else
                    httpRequest.receiveSize++;
            }
        }

        container.inCall = false;
    }

    @Override
    public void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        onReturnExit(index, version, param, instance, 0);
    }

    @Override
    public BufferedReader onReturnExitReader(Object param, Object retVal) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return (BufferedReader) retVal;

        container.inCall = true;
        setRecursive2(false);

        IRequest request = getRequest();
        if (request != null) {
            HttpServletRawRequest rawRequest = request.getRawRequest();
            if (!rawRequest.receiveSizeSet)
                retVal = new ProbeReader((BufferedReader) retVal, rawRequest);
        }

        container.inCall = false;

        return (BufferedReader) retVal;
    }

    @Override
    public PrintWriter onReturnExitWriter(Object param, Object retVal) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return (PrintWriter) retVal;

        container.inCall = true;
        setRecursive2(false);

        IRequest request = getRequest();
        if (request != null) {
            HttpServletRawRequest rawRequest = request.getRawRequest();
            if (!rawRequest.sendSizeSet)
                retVal = new ProbeWriter((PrintWriter) retVal, rawRequest);
        }

        container.inCall = false;

        return (PrintWriter) retVal;
    }

    @Override
    public Object onCallEnter(int index, int version, Object instance, Object callee, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || isRecursive())
            return null;

        container.inCall = true;

        IHttpServletBridge bridge = bridgeHolder.get(params[0]);

        TraceTag tag = null;
        int variant = 0;
        String tagString = bridge.getTag(params[0]);
        if (tagString != null) {
            tag = TraceTag.fromString(tagString);
            variant = tag.variant;
        }

        int contentLength = bridge.getContentLength(params[0]);
        HttpServletRawRequest httpRequest = new HttpServletRawRequest(instance, params[0], params[1], bridge.getContextName(params[0]),
                bridge.getRequestURI(params[0]), bridge.getQueryString(params[0]), variant);

        container.scopes.deactivateAll();

        if (contentLength != -1) {
            httpRequest.receiveSize = contentLength;
            httpRequest.receiveSizeSet = true;
        }

        beginRequest(container, httpRequest, tag);
        httpRequest.startTime = context.getTimeSource().getCurrentTime();

        setRecursive(true);
        container.inCall = false;

        return httpRequest;
    }

    @Override
    public void onCallReturnExit(int index, int version, Object param, Object instance, Object callee, Object retVal) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return;

        container.inCall = true;
        setRecursive(false);

        if (retVal instanceof Throwable)
            logError(container, 500, ((Throwable) retVal).getMessage());

        if (param instanceof HttpServletRawRequest) {
            HttpServletRawRequest httpRequest = (HttpServletRawRequest) param;
            httpRequest.endTime = context.getTimeSource().getCurrentTime();
            endRequest(container, null);

            if (!httpRequest.requestTimeSet) {
                IHttpServletBridge bridge = bridgeHolder.get(httpRequest.response);
                bridge.setRequestTime(httpRequest.response, httpRequest.endTime - httpRequest.startTime);
                httpRequest.receiveSizeSet = true;
            }
        }

        container.scopes.activateAll();
        container.inCall = false;
    }

    @Override
    public void onCallThrowExit(int index, int version, Object param, Object instance, Object callee, Throwable exception) {
        onCallReturnExit(index, version, param, instance, callee, exception);
    }

    @Override
    protected EntryPointProbeCollector doCreateCollector(int index, String combineId, ICallPath callPath,
                                                         String name, StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata, boolean primary, boolean leaf) {
        return new HttpServletProbeCollector(index, this, name, combineId, callPath, root, parent, metadata, primary, leaf);
    }

    @Override
    protected IRequest mapScope(Object rawRequest) {
        HttpServletRawRequest request = (HttpServletRawRequest) rawRequest;
        return new Request("app:" + request.contextName, null);
    }

    private void logError(Container container, int status, String message) {
        long[] counters = container.counters;
        counters[AppStackCounterType.ERRORS_COUNT.ordinal()]++;

        HttpServletProbeCollector collector = getCollector();
        IRequest request = getRequest();
        if (collector == null || request == null)
            return;

        collector.logError(request, status, message);
    }

    private boolean isRecursive2() {
        HttpEntryPointInfo entryPoint = slot.get();
        return entryPoint.recursive2;
    }

    private void setRecursive2(boolean value) {
        HttpEntryPointInfo entryPoint = slot.get();
        entryPoint.recursive2 = value;
    }

    private static class ProbeWriter extends PrintWriter {
        private final HttpServletRawRequest request;

        public ProbeWriter(PrintWriter writer, HttpServletRawRequest request) {
            super(writer);

            Assert.notNull(request);

            this.request = request;
        }

        @Override
        public void write(int c) {
            request.inCall = true;
            incrementSendSize(2);
            super.write(c);
            request.inCall = false;
        }

        @Override
        public void write(char buf[], int off, int len) {
            request.inCall = true;
            incrementSendSize(len * 2);
            super.write(buf, off, len);
            request.inCall = false;
        }

        @Override
        public void write(String s, int off, int len) {
            request.inCall = true;
            incrementSendSize(len * 2);
            super.write(s, off, len);
            request.inCall = false;
        }

        private void incrementSendSize(long value) {
            if (!request.sendSizeSet)
                request.sendSize += value;
        }
    }

    private static class ProbeReader extends BufferedReader {
        private final HttpServletRawRequest request;
        private long receiveSize;
        private long markReceiveSize;

        public ProbeReader(BufferedReader reader, HttpServletRawRequest request) {
            super(reader);

            Assert.notNull(request);

            this.request = request;
        }

        @Override
        public int read() throws IOException {
            request.inCall = true;

            int n = super.read();
            if (n != -1)
                incrementReceiveSize(2);

            request.inCall = false;
            return n;
        }

        @Override
        public int read(char cbuf[], int off, int len) throws IOException {
            request.inCall = true;

            int n = super.read(cbuf, off, len);
            if (n != -1)
                incrementReceiveSize(len * 2);

            request.inCall = false;
            return n;
        }

        @Override
        public String readLine() throws IOException {
            request.inCall = true;
            String res = super.readLine();
            if (res != null)
                incrementReceiveSize(res.length() * 2);
            request.inCall = false;
            return res;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = super.skip(n);
            incrementReceiveSize(skipped * 2);
            return skipped;
        }

        @Override
        public void mark(int readlimit) throws IOException {
            markReceiveSize = receiveSize;
            super.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            incrementReceiveSize(markReceiveSize - receiveSize);
            super.reset();
        }

        private void incrementReceiveSize(long value) {
            if (!request.receiveSizeSet)
                request.receiveSize += value;

            receiveSize += value;
        }
    }

    private static class HttpEntryPointInfo extends EntryPointInfo {
        private boolean recursive2;
    }
}
