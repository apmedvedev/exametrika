/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;


import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.meters.ILog;
import com.exametrika.spi.aggregator.common.meters.LogEvent;
import com.exametrika.spi.profiler.AbstractProbeCollector;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalSlot;
import com.exametrika.spi.profiler.ITransactionInfo;
import com.exametrika.spi.profiler.LogProbeEvent;
import com.exametrika.spi.profiler.boot.ThreadLocalContainer;
import com.exametrika.spi.profiler.config.LogProbeConfiguration;


/**
 * The {@link LogProbeCollector} is a log probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class LogProbeCollector extends AbstractProbeCollector {
    private final LogProbeConfiguration configuration;
    private final IThreadLocalSlot slot;
    private ILog log;

    public static class CollectorInfo {
        public LogProbeCollector collector;
    }

    public LogProbeCollector(LogProbeConfiguration configuration, IProbeContext context, IScope scope,
                             IThreadLocalSlot slot, ThreadLocalContainer container, JsonObject metadata, String componentType) {
        super(configuration, context, scope, container, metadata, true, componentType);

        this.configuration = configuration;
        this.slot = slot;

        createMeters();
    }

    @Override
    public void begin() {
        super.begin();

        CollectorInfo info = getSlotInfo(slot);
        info.collector = this;
    }

    @Override
    public void end() {
        CollectorInfo info = getSlotInfo(slot);
        info.collector = null;

        super.end();
    }

    public void measure(LogProbeEvent logEvent) {
        if (log == null)
            return;

        long transactionId = 0;
        ITransactionInfo transaction = context.getCurrentTransaction();
        if (transaction != null)
            transactionId = transaction.getId();

        JsonObjectBuilder parameters = Json.object()
                .put("logger", logEvent.getLogger())
                .put("level", logEvent.getLevel())
                .putIf("transactionId", transactionId, transactionId != 0)
                .put("thread", logEvent.getThread())
                .toObjectBuilder();

        LogEvent event = new ApplicationLogEvent(log.getId(), "log", logEvent.getTime(), logEvent.getMessage(),
                logEvent.getException(), parameters);
        log.measure(event);

        if (Thread.currentThread().getClass().getClassLoader() == getClass().getClassLoader())
            extract();
    }

    @Override
    protected void createMeters() {
        if (configuration.getLog().isEnabled()) {
            Assert.notNull(componentType);
            this.log = meters.addLog(componentType + ".log", configuration.getLog());
        }
    }

    protected CollectorInfo getSlotInfo(IThreadLocalSlot slot) {
        return slot.get();
    }
}
