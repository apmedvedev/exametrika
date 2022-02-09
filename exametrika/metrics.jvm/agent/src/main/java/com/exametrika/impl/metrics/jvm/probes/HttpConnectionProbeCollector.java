/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.metrics.jvm.config.HttpConnectionProbeConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.model.MeasurementIdProvider;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.metrics.jvm.probes.HttpConnectionProbe.HttpConnectionRawRequest;
import com.exametrika.impl.profiler.probes.ExitPointProbeCalibrateInfo;
import com.exametrika.impl.profiler.probes.ExitPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.aggregator.common.meters.ILog;
import com.exametrika.spi.aggregator.common.meters.LogEvent;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.ITransactionInfo;


/**
 * The {@link HttpConnectionProbeCollector} is a HTTP connection probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HttpConnectionProbeCollector extends ExitPointProbeCollector {
    private final HttpConnectionProbeConfiguration configuration;
    private HttpConnectionRawRequest request;
    private ICounter timeCounter;
    private ICounter receiveBytesCounter;
    private ICounter sendBytesCounter;
    private ILog errorsLog;

    public HttpConnectionProbeCollector(HttpConnectionProbeConfiguration configuration,
                                        int index, String name, UUID stackId, ICallPath callPath, StackProbeRootCollector root,
                                        StackProbeCollector parent, JsonObject metadata, ExitPointProbeCalibrateInfo calibrateInfo, boolean leaf) {
        super(configuration, index, name, stackId, callPath, root, parent, metadata, calibrateInfo, leaf);

        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    public void logError(IRequest request, int status, String message) {
        if (errorsLog == null || !isLeaf())
            return;

        IProbeContext context = getRoot().getContext();

        ITransactionInfo transaction = getRoot().getTransaction();
        long transactionId = 0;
        if (transaction != null)
            transactionId = transaction.getId();

        LogEvent event = new HttpConnectionErrorLogEvent(errorsLog.getId(), "httpConnectionError", context.getTimeService().getCurrentTime(), transactionId,
                Thread.currentThread().getName(), message, getRoot().getScope(), status, request);

        errorsLog.measure(event);
    }

    public void addReceiveSize(long value) {
        if (receiveBytesCounter != null) {
            if (getParent() instanceof HttpConnectionProbeCollector) {
                HttpConnectionProbeCollector parent = (HttpConnectionProbeCollector) getParent();
                parent.receiveBytesCounter.measureDelta(value);
            }

            receiveBytesCounter.measureDelta(value);
        }
    }

    @Override
    protected void beginMeasure(Object param) {
        request = ((IRequest) param).getRawRequest();
        if (request.isConnect())
            return;

        super.beginMeasure(param);
    }

    @Override
    public void endMeasure() {
        if (request.isConnect())
            return;

        super.endMeasure();
    }

    @Override
    protected void doCreateMeters() {
        if (configuration.getTimeCounter().isEnabled())
            timeCounter = getMeters().addMeter("app.http.time", configuration.getTimeCounter(), null);
        if (configuration.getReceiveBytesCounter().isEnabled())
            receiveBytesCounter = getMeters().addMeter("app.http.receive.bytes", configuration.getReceiveBytesCounter(), null);
        if (configuration.getSendBytesCounter().isEnabled())
            sendBytesCounter = getMeters().addMeter("app.http.send.bytes", configuration.getSendBytesCounter(), null);
        if (configuration.getErrorsLog().isEnabled())
            errorsLog = getMeters().addLog(new MeasurementIdProvider(new NameMeasurementId(((NameMeasurementId) getMeters().getId()).getScope(),
                    MetricName.root(), getComponentType() + ".errors")), configuration.getErrorsLog());
    }

    @Override
    protected void doClearMeters() {
        timeCounter = null;
        receiveBytesCounter = null;
        sendBytesCounter = null;
        errorsLog = null;
    }

    @Override
    protected void doEndMeasure(IRequest request) {
        HttpConnectionRawRequest httpRequest = request.getRawRequest();

        if (timeCounter != null)
            timeCounter.measureDelta(httpRequest.getDelta());
        if (receiveBytesCounter != null && httpRequest.isReceiveSizeSet())
            receiveBytesCounter.measureDelta(httpRequest.getReceiveSize());
        if (sendBytesCounter != null)
            sendBytesCounter.measureDelta(httpRequest.getSendSize());
    }
}
