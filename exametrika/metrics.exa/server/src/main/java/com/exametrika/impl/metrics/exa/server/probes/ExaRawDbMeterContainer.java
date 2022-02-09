/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.probes;

import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.metrics.exa.server.config.ExaAggregatorProbeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.aggregator.common.meters.IGauge;
import com.exametrika.spi.aggregator.common.meters.IInfo;
import com.exametrika.spi.aggregator.common.meters.ILog;
import com.exametrika.spi.aggregator.common.meters.LogEvent;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.profiler.IProbeContext;

/**
 * The {@link ExaRawDbMeterContainer} is an Exa rawDb meter container.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ExaRawDbMeterContainer extends MeterContainer {
    private final ExaAggregatorProbeConfiguration configuration;
    private IInfo memoryManager;
    private IGauge pagePool;
    private ICounter fileReadTime;
    private ICounter fileReadBytes;
    private ICounter fileWriteTime;
    private ICounter fileWriteBytes;
    private IGauge fileCurrentLoaded;
    private ICounter fileLoaded;
    private ICounter fileUnloaded;
    private ICounter transactionLogFlushTime;
    private ICounter transactionLogFlushBytes;
    private IGauge transactionQueue;
    private ICounter transactionTime;
    private ILog transactionErrors;

    public ExaRawDbMeterContainer(ExaAggregatorProbeConfiguration configuration, NameMeasurementId id, IProbeContext context,
                                  IInstanceContextProvider contextProvider, JsonObject metadata) {
        super(id, context, contextProvider);

        Assert.notNull(configuration);

        this.configuration = configuration;

        createMeters();
        setMetadata(Json.object(metadata)
                .put("node", context.getConfiguration().getNodeName())
                .toObject());
    }

    public void onDatabase(JsonObject resourceAllocatorInfo, int currentFileCount, int pagePoolSize,
                           int transactionQueueSize) {
        memoryManager.measure(resourceAllocatorInfo);
        fileCurrentLoaded.measure(currentFileCount);
        pagePool.measure(pagePoolSize);
        transactionQueue.measure(transactionQueueSize);
    }

    public void onBeforeFileRead() {
        fileReadTime.beginMeasure(getTime());
    }

    public void onAfterFileRead(int size) {
        fileReadTime.endMeasure(getTime());
        fileReadBytes.measureDelta(size);
    }

    public void onBeforeFileWritten() {
        fileWriteTime.beginMeasure(getTime());
    }

    public void onAfterFileWritten(int size) {
        fileWriteTime.endMeasure(getTime());
        fileWriteBytes.measureDelta(size);
    }

    public void onFileLoaded() {
        fileLoaded.measureDelta(1);
    }

    public void onFileUnloaded() {
        fileUnloaded.measureDelta(1);
    }

    public void onBeforeLogFlushed() {
        transactionLogFlushTime.beginMeasure(getTime());
    }

    public void onAfterLogFlushed(long size) {
        transactionLogFlushTime.endMeasure(getTime());
        transactionLogFlushBytes.measureDelta(size);
    }

    public void onTransactionStarted() {
        transactionTime.beginMeasure(getTime());
    }

    public void onTransactionCommitted() {
        transactionTime.endMeasure(getTime());
    }

    public void onTransactionRolledBack(Throwable exception) {
        transactionTime.endMeasure(getTime());
        long time = ((IProbeContext) context).getTimeService().getCurrentTime();
        transactionErrors.measure(new LogEvent(transactionErrors.getId(), "error", time, null, exception, null, true));
    }

    protected void createMeters() {
        memoryManager = addInfo("exa.rawdb.memoryManager", null);
        pagePool = addMeter("exa.rawdb.pagePool", configuration.getGauge(), null);
        fileReadTime = addMeter("exa.rawdb.file.read.time", configuration.getTimeCounter(), null);
        fileReadBytes = addMeter("exa.rawdb.file.read.bytes", configuration.getCounter(), null);
        fileWriteTime = addMeter("exa.rawdb.file.write.time", configuration.getTimeCounter(), null);
        fileWriteBytes = addMeter("exa.rawdb.file.write.bytes", configuration.getCounter(), null);
        fileCurrentLoaded = addMeter("exa.rawdb.file.currentLoaded", configuration.getGauge(), null);
        fileLoaded = addMeter("exa.rawdb.file.loaded", configuration.getCounter(), null);
        fileUnloaded = addMeter("exa.rawdb.file.unloaded", configuration.getCounter(), null);
        transactionLogFlushTime = addMeter("exa.rawdb.transactionLog.flush.time", configuration.getTimeCounter(), null);
        transactionLogFlushBytes = addMeter("exa.rawdb.transactionLog.flush.bytes", configuration.getCounter(), null);
        transactionQueue = addMeter("exa.rawdb.transaction.queue", configuration.getGauge(), null);
        transactionTime = addMeter("exa.rawdb.transaction.time", configuration.getTimeCounter(), null);
        transactionErrors = addLog("exa.rawdb.transaction.errors.log", configuration.getTransactionErrors());
    }

    private long getTime() {
        if (Times.isTickCountAvaliable())
            return Times.getWallTime();
        else
            return System.nanoTime();
    }
}