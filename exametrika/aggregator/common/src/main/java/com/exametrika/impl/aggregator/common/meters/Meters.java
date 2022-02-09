/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.meters;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.meters.config.InstanceFieldConfiguration;
import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMeasurementIdProvider;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.common.json.Json;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.aggregator.common.fields.instance.InstanceFieldFactory;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;
import com.exametrika.spi.aggregator.common.meters.IFieldFactory;
import com.exametrika.spi.aggregator.common.meters.IMeasurementContext;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.IMeter;
import com.exametrika.spi.aggregator.common.meters.IMeterContainer;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.FieldConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.GaugeConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.InfoConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.MeterConfiguration;


/**
 * The {@link Meters} is a factory for meters.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Meters {
    public static <T extends IMeter> T createMeter(IMeterContainer meterContainer, String metricType,
                                                   MeterConfiguration configuration, IMeasurementIdProvider idProvider, IMeasurementProvider measurementProvider,
                                                   IInstanceContextProvider instanceContextProvider, IMeasurementContext context) {
        Assert.notNull(configuration);
        Assert.notNull(idProvider);
        Assert.notNull(instanceContextProvider);
        Assert.notNull(context);

        if (configuration instanceof GaugeConfiguration) {
            GaugeConfiguration gaugeConfiguration = (GaugeConfiguration) configuration;

            return (T) new Gauge(metricType, measurementProvider, createFieldCollectors(gaugeConfiguration.getFields(),
                    instanceContextProvider, idProvider), instanceContextProvider);
        } else if (configuration instanceof CounterConfiguration) {
            CounterConfiguration counterConfiguration = (CounterConfiguration) configuration;

            return (T) new Counter(metricType, measurementProvider, counterConfiguration.getUseDeltas(), counterConfiguration.getSmoothingSize(),
                    createFieldCollectors(counterConfiguration.getFields(), instanceContextProvider, idProvider), instanceContextProvider);
        } else if (configuration instanceof LogConfiguration) {
            LogConfiguration logConfiguration = (LogConfiguration) configuration;
            if (metricType != null)
                idProvider = new LogMeasurementIdProvider(idProvider, metricType);
            return (T) new Log(meterContainer, logConfiguration, idProvider, measurementProvider, context);
        } else if (configuration instanceof InfoConfiguration)
            return (T) new Info(metricType, measurementProvider);
        else
            return Assert.error();
    }

    public static IFieldCollector[] createFieldCollectors(List<FieldConfiguration> fieldConfigurations,
                                                          IInstanceContextProvider instanceContextProvider, IMeasurementIdProvider idProvider) {
        IFieldCollector[] fieldCollectors = new IFieldCollector[fieldConfigurations.size()];

        for (int i = 0; i < fieldConfigurations.size(); i++)
            fieldCollectors[i] = createFieldCollector(fieldConfigurations.get(i), instanceContextProvider, idProvider);

        return fieldCollectors;
    }

    public static void buildExceptionStackTrace(Throwable e, int maxStackTraceDepth, int maxMessageSize, Json json, boolean full) {
        List<Throwable> exceptions = getExceptionList(e);
        for (int i = 0; i < exceptions.size(); i++) {
            Throwable exception = exceptions.get(i);
            int count = maxStackTraceDepth;

            if (i > 0 && full) {
                int m = getPrintStackTraceSize(exceptions.get(i - 1), exception);

                if (count > m)
                    count = m;
            }

            json.put("class", exception.getClass().getName());
            if (exception.getMessage() != null)
                json.put("message", Strings.truncate(exception.getMessage(), maxMessageSize, true));

            if (full)
                buildStackTrace(json, exception.getStackTrace(), 0, count);

            if (i < exceptions.size() - 1)
                json = json.putObject("cause");
        }
    }

    public static String shorten(ICallPath callPath) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (IMetricName metric : callPath.getSegments()) {
            if (first)
                first = false;
            else
                builder.append(ICallPath.SEPARATOR);

            if (metric.getSegments().size() <= 2)
                builder.append(metric.toString());
            else
                builder.append(metric.getSegments().get(metric.getSegments().size() - 2) + '.' +
                        metric.getSegments().get(metric.getSegments().size() - 1));
        }

        return builder.toString();
    }

    private Meters() {
    }

    private static IFieldCollector createFieldCollector(FieldConfiguration fieldConfiguration,
                                                        IInstanceContextProvider instanceContextProvider, IMeasurementIdProvider idProvider) {
        IFieldFactory fieldFactory;
        if (fieldConfiguration instanceof InstanceFieldConfiguration)
            fieldFactory = new InstanceFieldFactory((InstanceFieldConfiguration) fieldConfiguration, idProvider,
                    instanceContextProvider);
        else
            fieldFactory = fieldConfiguration.createFactory();
        return fieldFactory.createCollector();
    }

    private static List<Throwable> getExceptionList(Throwable e) {
        List<Throwable> list = new ArrayList<Throwable>();
        list.add(e);

        while (e.getCause() != null) {
            e = e.getCause();
            list.add(e);
        }

        return list;
    }

    private static int getPrintStackTraceSize(Throwable prevException, Throwable exception) {
        StackTraceElement[] trace = exception.getStackTrace();
        StackTraceElement[] prevTrace = prevException.getStackTrace();

        int m = trace.length - 1;
        int n = prevTrace.length - 1;
        while (m >= 0 && n >= 0 && trace[m].equals(prevTrace[n])) {
            m--;
            n--;
        }

        return m + 1;
    }

    private static void buildStackTrace(Json json, StackTraceElement[] trace, int offset, int count) {
        count = Math.min(trace.length, count + offset);

        json = json.putArray("stackTrace");

        for (int i = offset; i < count; i++) {
            json.addObject()
                    .put("class", trace[i].getClassName())
                    .put("method", trace[i].getMethodName())
                    .put("file", trace[i].getFileName())
                    .put("line", trace[i].getLineNumber())
                    .end();
        }

        if (trace.length - count > 0)
            json.addObject().put("more", trace.length - count).end();
    }
}
