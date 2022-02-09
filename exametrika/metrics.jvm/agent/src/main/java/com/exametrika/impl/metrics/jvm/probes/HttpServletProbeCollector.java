/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.metrics.jvm.probes.HttpServletProbe.HttpServletRawRequest;
import com.exametrika.impl.profiler.probes.EntryPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.spi.aggregator.common.meters.LogEvent;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.ITransactionInfo;


/**
 * The {@link HttpServletProbeCollector} is a HTTP servlet probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HttpServletProbeCollector extends EntryPointProbeCollector {
    public HttpServletProbeCollector(int index, HttpServletProbe probe, String name, String combineId, ICallPath callPath,
                                     StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata, boolean primary, boolean leaf) {
        super(index, probe, name, combineId, callPath, root, parent, metadata, primary, leaf);
    }

    public void logError(IRequest request, int status, String message) {
        if (errorsLog == null)
            return;

        IProbeContext context = getRoot().getContext();

        ITransactionInfo transaction = getRoot().getTransaction();
        long transactionId = 0;
        if (transaction != null)
            transactionId = transaction.getId();

        LogEvent event = new HttpServletErrorLogEvent(errorsLog.getId(), "httpServletError", context.getTimeService().getCurrentTime(), transactionId,
                Thread.currentThread().getName(), message, status, request);

        errorsLog.measure(event);
    }

    @Override
    protected void doEndMeasure(IRequest request, long currentThreadCpuTime) {
        HttpServletRawRequest httpRequest = request.getRawRequest();

        if (timeCounter != null)
            timeCounter.measureDelta(httpRequest.getEndTime() - httpRequest.getStartTime());
        if (receiveBytesCounter != null)
            receiveBytesCounter.measureDelta(httpRequest.getReceiveSize());
        if (sendBytesCounter != null)
            sendBytesCounter.measureDelta(httpRequest.getSendSize());
    }
}
