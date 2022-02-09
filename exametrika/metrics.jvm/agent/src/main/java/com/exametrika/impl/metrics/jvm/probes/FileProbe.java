/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.UUID;

import sun.nio.ch.FileChannelImpl;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.metrics.jvm.config.FileProbeConfiguration;
import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Fields;
import com.exametrika.common.utils.Fields.IField;
import com.exametrika.impl.metrics.jvm.boot.FileProbeInterceptor;
import com.exametrika.impl.profiler.probes.ExitPointProbe;
import com.exametrika.impl.profiler.probes.ExitPointProbeCalibrateInfo;
import com.exametrika.impl.profiler.probes.ExitPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.metrics.jvm.IFileRawRequest;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.Request;


/**
 * The {@link FileProbe} is a file probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class FileProbe extends ExitPointProbe {
    private static final IField fileInputStreamPathField = Fields.get(FileInputStream.class, "path");
    private static final IField fileOutputStreamPathField = Fields.get(FileOutputStream.class, "path");
    private static final IField fileChannelPathField = Fields.get(FileChannelImpl.class, "path");
    private static final IField randomAccessFilePathField = Fields.get(RandomAccessFile.class, "path");
    private final FileProbeConfiguration configuration;

    public static class FileRawRequest extends Request implements IFileRawRequest {
        private long size;
        private final boolean read;
        private final boolean hasParams;
        private long startTime;
        private long delta;

        public FileRawRequest(String path, int size, boolean read, boolean hasParams) {
            super(path != null ? path : "<unknown>", null);

            this.size = size;
            this.read = read;
            this.hasParams = hasParams;
        }

        public long getSize() {
            return size;
        }

        @Override
        public String getPath() {
            return getName();
        }

        @Override
        public boolean isRead() {
            return read;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getDelta() {
            return delta;
        }
    }

    public FileProbe(FileProbeConfiguration configuration, IProbeContext context, int index) {
        super(configuration, context, index, "fileProbe");

        this.configuration = configuration;
    }

    @Override
    public void start() {
        FileProbeInterceptor.interceptor = this;
    }

    @Override
    public void stop() {
        FileProbeInterceptor.interceptor = null;
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || container.top == null || isRecursive())
            return null;

        container.inCall = true;

        FileRawRequest request = createRequest(index, instance, params);
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

        FileRawRequest request = (FileRawRequest) param;
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
                                                        StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata, ExitPointProbeCalibrateInfo calibrateInfo,
                                                        boolean leaf) {
        return new FileProbeCollector(configuration, index, name, stackId, callPath, root, parent, metadata, calibrateInfo,
                leaf);
    }

    private FileRawRequest createRequest(int index, Object instance, Object[] params) {
        boolean read;
        if (index >= 0) {
            IJoinPoint joinPoint = context.getJoinPointProvider().findJoinPoint(index, -1);
            if (joinPoint == null)
                return null;

            read = joinPoint.getMethodName().startsWith("read");
        } else
            read = false;

        String path = getPath(instance);
        boolean hasParams = params.length > 0;

        int size = 0;
        if (!read && (instance instanceof FileOutputStream || instance instanceof RandomAccessFile)) {
            if (params[0] instanceof byte[]) {
                if (params.length == 3)
                    size = (Integer) params[2];
                else
                    size = ((byte[]) params[0]).length;
            } else
                size = 1;
        }

        return new FileRawRequest(path, size, read, hasParams);
    }

    @Override
    protected IRequest mapRequest(IScope scope, Object rawRequest) {
        return (IRequest) rawRequest;
    }

    @Override
    protected Object createCalibratingRequest() {
        return new FileRawRequest("test", 0, false, true);
    }

    private void updateRequest(Container container, FileRawRequest request, Object instance, Object retVal) {
        if (instance instanceof FileInputStream)
            request.size = request.hasParams ? ((Number) retVal).intValue() : 1;
        else if (instance instanceof FileChannel)
            request.size = ((Number) retVal).longValue();
        else if (instance instanceof RandomAccessFile) {
            if (request.read)
                request.size = request.hasParams ? ((Number) retVal).intValue() : 1;
        }

        if (request.size == -1)
            request.size = 0;

        long[] counters = container.counters;
        counters[AppStackCounterType.IO_COUNT.ordinal()]++;
        counters[AppStackCounterType.IO_BYTES.ordinal()] += request.size;
        counters[AppStackCounterType.IO_TIME.ordinal()] += request.delta;
        counters[AppStackCounterType.FILE_COUNT.ordinal()]++;
        counters[AppStackCounterType.FILE_BYTES.ordinal()] += request.size;
        counters[AppStackCounterType.FILE_TIME.ordinal()] += request.delta;
        if (request.read) {
            counters[AppStackCounterType.FILE_READ_COUNT.ordinal()]++;
            counters[AppStackCounterType.FILE_READ_BYTES.ordinal()] += request.size;
            counters[AppStackCounterType.FILE_READ_TIME.ordinal()] += request.delta;
        } else {
            counters[AppStackCounterType.FILE_WRITE_COUNT.ordinal()]++;
            counters[AppStackCounterType.FILE_WRITE_BYTES.ordinal()] += request.size;
            counters[AppStackCounterType.FILE_WRITE_TIME.ordinal()] += request.delta;
        }
    }

    private static String getPath(Object instance) {
        if (instance instanceof FileInputStream && fileInputStreamPathField != null)
            return fileInputStreamPathField.getObject(instance);
        else if (instance instanceof FileOutputStream && fileOutputStreamPathField != null)
            return fileOutputStreamPathField.getObject(instance);
        else if (instance instanceof FileChannel && fileChannelPathField != null)
            return fileChannelPathField.getObject(instance);
        else if (instance instanceof RandomAccessFile && randomAccessFilePathField != null)
            return randomAccessFilePathField.getObject(instance);
        else
            return "";
    }
}
