/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.meters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.values.AggregationContext;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.aggregator.common.values.IComponentTypeAggregationSchema;
import com.exametrika.spi.aggregator.common.values.IComponentValueBuilder;


/**
 * The {@link AggregatingMeasurementHandler} is a aggregating measurement handler that preaggregates measurements for specified extraction period.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AggregatingMeasurementHandler implements IMeasurementHandler, ITimerListener {
    private static final ILogger logger = Loggers.get(AggregatingMeasurementHandler.class);
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final int EXTRACTION_DELAY = 5000;
    private static final int NO_MEASUREMENT_CYCLE_COUNT = 1;
    private final IAggregationSchema schema;
    private final long extractionPeriod;
    private final ITimeService timeService;
    private final IMeasurementHandler measurementHandler;
    private final Map<IMeasurementId, MeasurementInfo> measurements = new LinkedHashMap<IMeasurementId, MeasurementInfo>();
    private List<MeasurementSet> queue = new ArrayList<MeasurementSet>();
    private long nextExtractionTime;
    private boolean measurementsRequested;

    public AggregatingMeasurementHandler(IAggregationSchema schema, long extractionPeriod, ITimeService timeService,
                                         IMeasurementHandler measurementHandler) {
        Assert.notNull(schema);
        Assert.notNull(timeService);
        Assert.notNull(measurementHandler);

        this.schema = schema;
        this.extractionPeriod = extractionPeriod;
        this.timeService = timeService;
        this.measurementHandler = measurementHandler;

        long currentTime = timeService.getCurrentTime();
        nextExtractionTime = (currentTime / extractionPeriod + 1) * extractionPeriod + EXTRACTION_DELAY;
    }

    public synchronized void requestMeasurements() {
        measurementsRequested = true;
    }

    @Override
    public boolean canHandle() {
        return measurementHandler.canHandle();
    }

    @Override
    public void handle(MeasurementSet measurements) {
        Assert.notNull(measurements);

        if (measurements.getSchemaVersion() != schema.getVersion())
            return;

        synchronized (this) {
            queue.add(measurements);
        }
    }

    @Override
    public void onTimer() {
        aggregate();
        extract();
    }

    private void aggregate() {
        List<MeasurementSet> queue = null;
        synchronized (this) {
            if (this.queue.isEmpty())
                return;

            queue = this.queue;
            this.queue = new ArrayList<MeasurementSet>();
        }

        AggregationContext context = new AggregationContext();
        for (MeasurementSet measurements : queue) {
            context.setTime(measurements.getTime());

            for (Measurement measurement : measurements.getMeasurements()) {
                context.setPeriod(measurement.getPeriod());
                aggregate(measurements.getTime(), measurement, context);
            }
        }
    }

    private void aggregate(long time, Measurement measurement, AggregationContext context) {
        MeasurementInfo info = measurements.get(measurement.getId());
        if (info == null) {
            info = new MeasurementInfo(schema.findComponentType(measurement.getId().getComponentType()));
            measurements.put(measurement.getId(), info);
        }

        if (!info.hasMeasurement)
            info.startTime = time - measurement.getPeriod();

        info.schema.getAggregator().aggregate(info.builder, measurement.getValue(), context);

        info.hasMeasurement = true;
        info.readyToRemoveCounter = 0;
    }

    private void extract() {
        if (!measurementHandler.canHandle())
            return;

        long currentTime = timeService.getCurrentTime();

        boolean measurementsRequested = false;
        synchronized (this) {
            measurementsRequested = this.measurementsRequested;
            this.measurementsRequested = false;
        }

        if (measurementsRequested || currentTime >= nextExtractionTime) {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.startExtract());

            nextExtractionTime = (currentTime / extractionPeriod + 1) * extractionPeriod + EXTRACTION_DELAY;

            List<Measurement> measurements = new ArrayList<Measurement>();
            for (Iterator<Map.Entry<IMeasurementId, MeasurementInfo>> it = this.measurements.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<IMeasurementId, MeasurementInfo> entry = it.next();
                MeasurementInfo info = entry.getValue();
                if (!info.hasMeasurement) {
                    if (info.readyToRemoveCounter >= NO_MEASUREMENT_CYCLE_COUNT)
                        it.remove();
                    else
                        info.readyToRemoveCounter++;

                    continue;
                }

                measurements.add(new Measurement(entry.getKey(), info.builder.toValue(true), currentTime - info.startTime, null));
                info.builder.clear();
                info.hasMeasurement = false;

                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.extract(entry.getKey().toString()));
            }

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.endExtract());

            if (measurementsRequested || !measurements.isEmpty()) {
                MeasurementSet set = new MeasurementSet(measurements, null, schema.getVersion(), currentTime, MeasurementSet.RESPONSE_FLAG);
                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, messages.extracted(set));
                measurementHandler.handle(set);
            }
        }
    }

    private static class MeasurementInfo {
        private final IComponentTypeAggregationSchema schema;
        private final IComponentValueBuilder builder;
        private boolean hasMeasurement;
        private int readyToRemoveCounter;
        private long startTime;

        public MeasurementInfo(IComponentTypeAggregationSchema schema) {
            Assert.notNull(schema);

            this.schema = schema;
            this.builder = schema.getConfiguration().createBuilder();
        }
    }

    private interface IMessages {
        @DefaultMessage("Extraction has been started.")
        ILocalizedMessage startExtract();

        @DefaultMessage("Extracting: {0}.")
        ILocalizedMessage extract(String name);

        @DefaultMessage("Extraction has been ended.")
        ILocalizedMessage endExtract();

        @DefaultMessage("Extracted measurements: {0}.")
        ILocalizedMessage extracted(MeasurementSet measurements);
    }
}
