/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.meters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.api.aggregator.common.meters.config.LogMeterConfiguration;
import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.api.aggregator.common.model.IMeasurementIdProvider;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.ObjectValue;
import com.exametrika.spi.aggregator.common.meters.ILog;
import com.exametrika.spi.aggregator.common.meters.ILogEvent;
import com.exametrika.spi.aggregator.common.meters.ILogFilter;
import com.exametrika.spi.aggregator.common.meters.ILogProvider;
import com.exametrika.spi.aggregator.common.meters.IMeasurementContext;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.IMeter;
import com.exametrika.spi.aggregator.common.meters.IMeterContainer;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;


/**
 * The {@link Log} is a log.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Log implements ILog {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(Log.class);
    private final LogConfiguration configuration;
    private final IMeasurementIdProvider idProvider;
    private final IMeasurementProvider provider;
    private final ILogFilter filter;
    private final ILogFilter postFilter;
    private final ILogProvider transformer;
    private final IMeasurementContext context;
    private final List<MeterInfo> meters;
    private long startTime;
    private int rateCount;
    private int stackTraceRateCount;
    private JsonArrayBuilder builder = new JsonArrayBuilder();
    private long lastLogEventTime;
    private JsonObject metadata;

    public Log(IMeterContainer meterContainer, LogConfiguration configuration, IMeasurementIdProvider idProvider,
               IMeasurementProvider provider, IMeasurementContext context) {
        Assert.notNull(configuration);
        Assert.notNull(idProvider);
        Assert.notNull(context);

        if (!configuration.getMeters().isEmpty()) {
            Assert.notNull(meterContainer);

            meters = new ArrayList<MeterInfo>(configuration.getMeters().size());
            for (LogMeterConfiguration meter : configuration.getMeters()) {
                ILogFilter meterFilter = null;
                if (meter.getFilter() != null)
                    meterFilter = meter.getFilter().createFilter();

                ILogProvider meterProvider = null;
                if (meter.getProvider() != null)
                    meterProvider = meter.getProvider().createProvider();

                if (!(meter.getMeter() instanceof LogConfiguration))
                    meters.add(new MeterInfo(meterContainer.addMeter(meter.getMetricType(), meter.getMeter(), null),
                            meterFilter, meterProvider));
                else {
                    String metricType = LogConfiguration.getPrefix(idProvider.get().getComponentType(), meter.getMetricType()) +
                            meter.getMetricType();
                    meters.add(new MeterInfo(meterContainer.addLog(new LogMeasurementIdProvider(idProvider, metricType),
                            (LogConfiguration) meter.getMeter()), meterFilter, meterProvider));
                }
            }
        } else
            meters = null;

        this.configuration = configuration;
        this.idProvider = idProvider;
        this.provider = provider;

        if (configuration.getFilter() != null)
            this.filter = configuration.getFilter().createFilter();
        else
            this.filter = null;

        if (configuration.getPostFilter() != null)
            this.postFilter = configuration.getPostFilter().createFilter();
        else
            this.postFilter = null;

        if (configuration.getTransformer() != null)
            this.transformer = configuration.getTransformer().createProvider();
        else
            this.transformer = null;

        this.context = context;
    }

    @Override
    public IMeasurementId getId() {
        return idProvider.get();
    }

    @Override
    public String getMetricType() {
        return idProvider.get().getComponentType();
    }

    @Override
    public boolean hasProvider() {
        return provider != null;
    }

    @Override
    public void measure() {
        Assert.notNull(provider);

        ILogEvent value = null;
        try {
            value = (ILogEvent) provider.getValue();
        } catch (Exception e) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        }

        if (value != null)
            measure(value);
    }

    @Override
    public void measure(Object value) {
        measure((ILogEvent) value);
    }

    @Override
    public JsonObject getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(JsonObject metadata) {
        this.metadata = metadata;
    }

    @Override
    public IMetricValue extract(double approximationMultiplier, boolean force, boolean clear) {
        if (clear)
            sendMeasurement(null, lastLogEventTime, true);

        return null;
    }

    @Override
    public void measure(ILogEvent value) {
        Assert.notNull(value);

        if (filter != null && !filter.allow(value))
            return;

        if (meters != null) {
            for (MeterInfo info : meters) {
                if (info.filter != null && !info.filter.allow(value))
                    continue;

                Object meterValue = value;
                if (info.provider != null)
                    meterValue = info.provider.getValue(value);

                if (meterValue != null)
                    info.meter.measure(meterValue);
            }
        }

        if (startTime == 0 || value.getTime() >= startTime + 1000) {
            startTime = value.getTime();
            rateCount = configuration.getMaxRate();
            stackTraceRateCount = configuration.getMaxStackTraceRate();
        }

        if (rateCount <= 0)
            return;

        rateCount--;

        if (postFilter != null && !postFilter.allow(value))
            return;

        if (transformer != null) {
            value = (ILogEvent) transformer.getValue(value);
            if (value == null)
                return;
        }

        String message = Strings.truncate(value.getMessage(), configuration.getMaxMessageSize(), true);
        Json json = Json.object(value.getParameters())
                .putIf("type", value.getType(), !value.getType().isEmpty())
                .putIf("time", value.getTime(), value.getTime() != 0)
                .putIf("message", message, !message.isEmpty());

        boolean allowStackTrace = stackTraceRateCount > 0;
        if (value.hasStackTrace() && allowStackTrace)
            stackTraceRateCount--;

        if (value.getException() != null) {
            Throwable exception = value.getException();
            Meters.buildExceptionStackTrace(exception, configuration.getMaxStackTraceDepth(), configuration.getMaxMessageSize(),
                    json.putObject("exception"), allowStackTrace);

            if (allowStackTrace) {
                String errorLocation = value.getErrorLocation(context);
                if (errorLocation != null)
                    json.put("errorLocation", errorLocation);
            }
        }

        value.addParameters(allowStackTrace, configuration.getMaxStackTraceDepth(), json, context);

        JsonObject object = json.toObject();

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, messages.measurement(object));

        sendMeasurement(object, value.getTime(), false);
    }

    private void sendMeasurement(JsonObject object, long time, boolean force) {
        if (object != null) {
            builder.add(object);
            lastLogEventTime = time;
        }

        if ((context.getMeasurementHandler().canHandle() && (builder.size() >= configuration.getMaxBundleSize())) ||
                (!context.getMeasurementHandler().canHandle() && (builder.size() >= 10 * configuration.getMaxBundleSize())) ||
                (force && !builder.isEmpty())) {
            MeasurementSet measurements = new MeasurementSet(Collections.singletonList(new Measurement(idProvider.get(),
                    new ComponentValue(Collections.singletonList(new ObjectValue(builder.toJson())), metadata), 0, null)),
                    null, context.getSchemaVersion(), time, 0);
            builder.clear();

            context.getMeasurementHandler().handle(measurements);
        }
    }

    private static class MeterInfo {
        private final IMeter meter;
        private final ILogFilter filter;
        private final ILogProvider provider;

        public MeterInfo(IMeter meter, ILogFilter filter, ILogProvider provider) {
            Assert.notNull(meter);

            this.meter = meter;
            this.filter = filter;
            this.provider = provider;
        }
    }

    private interface IMessages {
        @DefaultMessage("Log measurement: {0}")
        ILocalizedMessage measurement(JsonObject measurement);
    }
}
