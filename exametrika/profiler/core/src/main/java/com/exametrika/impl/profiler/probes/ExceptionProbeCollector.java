/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;


import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.api.profiler.config.ExceptionProbeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.probes.ExceptionProbe.CollectorInfo;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.aggregator.common.meters.ILog;
import com.exametrika.spi.aggregator.common.meters.ILogEvent;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalSlot;
import com.exametrika.spi.profiler.ITransactionInfo;


/**
 * The {@link ExceptionProbeCollector} is an exception probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ExceptionProbeCollector extends BaseProbeCollector {
    private final ExceptionProbeConfiguration configuration;
    private final IThreadLocalSlot slot;
    private ILog log;
    private ILogEvent error;

    public ExceptionProbeCollector(ExceptionProbeConfiguration configuration, IProbeContext context, IScope scope,
                                   IThreadLocalSlot slot, Container container, JsonObject metadata, String componentType) {
        super(configuration, context, scope, container, metadata, true, componentType);

        Assert.notNull(slot);
        Assert.notNull(container);

        this.configuration = configuration;
        this.slot = slot;

        createMeters();
    }

    @Override
    public void extract() {
        logError();

        super.extract();
    }

    @Override
    public void begin() {
        super.begin();

        CollectorInfo info = slot.get();
        info.collector = this;
    }

    @Override
    public void end() {
        CollectorInfo info = slot.get();
        info.collector = null;

        super.end();
    }

    public void measure(Throwable exception) {
        if (log == null)
            return;

        if (error != null) {
            if (error.getException() == exception)
                return;

            logError();
        }

        error = createError(exception);
    }

    @Override
    protected void createMeters() {
        if (configuration.getLog().isEnabled()) {
            Assert.notNull(componentType);
            this.log = meters.addLog(componentType + ".log", configuration.getLog());
        }
    }

    private ILogEvent createError(Throwable exception) {
        long time = context.getTimeService().getCurrentTime();

        ITransactionInfo transaction = context.getCurrentTransaction();
        long transactionId = 0;
        if (transaction != null)
            transactionId = transaction.getId();

        String thread = Thread.currentThread().getName();
        return new ExceptionLogEvent(log.getId(), "exception", time, null, exception, Json.object()
                .putIf("transactionId", transactionId, transactionId != 0)
                .put("thread", thread).toObjectBuilder());
    }

    private void logError() {
        if (error == null)
            return;

        log.measure(error);

        long[] counters = container.counters;
        counters[AppStackCounterType.ERRORS_COUNT.ordinal()]++;

        error = null;
    }
}
