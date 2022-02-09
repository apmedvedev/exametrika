/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.common.meters.config.CountLogProviderConfiguration;
import com.exametrika.api.aggregator.common.meters.config.CustomHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.ErrorCountLogProviderConfiguration;
import com.exametrika.api.aggregator.common.meters.config.ErrorLogProviderConfiguration;
import com.exametrika.api.aggregator.common.meters.config.ExpressionLogFilterConfiguration;
import com.exametrika.api.aggregator.common.meters.config.ExpressionLogProviderConfiguration;
import com.exametrika.api.aggregator.common.meters.config.InstanceFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.LogMeterConfiguration;
import com.exametrika.api.aggregator.common.meters.config.LogarithmicHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.StandardFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.StatisticsFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.UniformHistogramFieldConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.FieldConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.GaugeConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.InfoConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogFilterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogProviderConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.MeterConfiguration;


/**
 * The {@link MetersSchemaLoader} is a configuration loader for meters schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MetersSchemaLoader extends AbstractExtensionLoader {
    private String metersPrefix;

    public MetersSchemaLoader(String metersPrefix) {
        Assert.notNull(metersPrefix);

        this.metersPrefix = metersPrefix;
    }

    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals(metersPrefix + "Gauge")) {
            boolean enabled = element.get("enabled");
            List<FieldConfiguration> fields = loadFields((JsonObject) element.get("fields", null), context);
            return new GaugeConfiguration(enabled, fields);
        } else if (type.equals(metersPrefix + "Counter")) {
            boolean enabled = element.get("enabled");
            List<FieldConfiguration> fields = loadFields((JsonObject) element.get("fields", null), context);
            boolean useDeltas = element.get("useDeltas");
            long smoothingSize = element.get("smoothingSize");
            return new CounterConfiguration(enabled, fields, useDeltas, (int) smoothingSize);
        } else if (type.equals(metersPrefix + "Log")) {
            boolean enabled = element.get("enabled");
            LogFilterConfiguration filter = loadLogFilter(element.get("filter", null), context);
            List<LogMeterConfiguration> meters = loadLogMeters((JsonObject) element.get("meters"), context);
            LogFilterConfiguration postFilter = loadLogFilter(element.get("postFilter", null), context);
            LogProviderConfiguration transformer = loadLogProvider(element.get("transformer", null), context);
            long maxStackTraceDepth = element.get("maxStackTraceDepth");
            long maxMessageSize = element.get("maxMessageSize");
            long maxRate = element.get("maxRate");
            long maxStackTraceRate = element.get("maxStackTraceRate");
            long maxBundleSize = element.get("maxBundleSize");
            return new LogConfiguration(enabled, filter, meters, postFilter, transformer, (int) maxStackTraceDepth, (int) maxMessageSize,
                    (int) maxRate, (int) maxStackTraceRate, (int) maxBundleSize);
        } else if (type.equals(metersPrefix + "Info")) {
            boolean enabled = element.get("enabled");
            return new InfoConfiguration(enabled);
        }
        if (type.equals(metersPrefix + "StandardFields"))
            return new StandardFieldConfiguration();
        else if (type.equals(metersPrefix + "StatisticsFields"))
            return new StatisticsFieldConfiguration();
        else if (type.equals(metersPrefix + "UniformHistogramFields")) {
            long minBound = element.get("minBound");
            long maxBound = element.get("maxBound");
            long binCount = element.get("binCount");
            return new UniformHistogramFieldConfiguration(minBound, maxBound, (int) binCount);
        } else if (type.equals(metersPrefix + "LogarithmicHistogramFields")) {
            long minBound = element.get("minBound");
            long binCount = element.get("binCount");
            return new LogarithmicHistogramFieldConfiguration(minBound, (int) binCount);
        } else if (type.equals(metersPrefix + "CustomHistogramFields")) {
            List<Long> bounds = new ArrayList<Long>();
            for (Object bound : (JsonArray) element.get("bounds"))
                bounds.add((Long) bound);

            return new CustomHistogramFieldConfiguration(bounds);
        } else if (type.equals(metersPrefix + "InstanceFields")) {
            long instanceCount = element.get("instanceCount");
            boolean max = element.get("max");
            return new InstanceFieldConfiguration((int) instanceCount, max);
        } else
            throw new InvalidConfigurationException();
    }

    private LogFilterConfiguration loadLogFilter(Object value, ILoadContext context) {
        if (value == null)
            return null;

        if (value instanceof String)
            return new ExpressionLogFilterConfiguration((String) value);
        else
            return load(null, null, value, context);
    }

    private LogProviderConfiguration loadLogProvider(Object value, ILoadContext context) {
        if (value == null)
            return null;

        if (value instanceof String)
            return new ExpressionLogProviderConfiguration((String) value);
        else {
            JsonObject element = (JsonObject) value;
            String type = getType(element);
            if (type.equals(metersPrefix + "CountLogProvider"))
                return new CountLogProviderConfiguration();
            else if (type.equals(metersPrefix + "ErrorCountLogProvider"))
                return new ErrorCountLogProviderConfiguration();
            else if (type.equals(metersPrefix + "ErrorLogProvider"))
                return new ErrorLogProviderConfiguration();
            else
                return load(null, null, value, context);
        }
    }

    private List<LogMeterConfiguration> loadLogMeters(JsonObject element, ILoadContext context) {
        List<LogMeterConfiguration> meters = new ArrayList<LogMeterConfiguration>();
        for (Map.Entry<String, Object> entry : element)
            meters.add(loadLogMeter(entry.getKey(), (JsonObject) entry.getValue(), context));

        return meters;
    }

    private LogMeterConfiguration loadLogMeter(String name, JsonObject element, ILoadContext context) {
        MeterConfiguration meter = load(null, null, element.get("meter"), context);
        LogFilterConfiguration filter = loadLogFilter(element.get("filter", null), context);
        LogProviderConfiguration provider = loadLogProvider(element.get("provider", null), context);
        return new LogMeterConfiguration(name, meter, filter, provider);
    }

    private List<FieldConfiguration> loadFields(JsonObject elements, ILoadContext context) {
        if (elements == null)
            return Arrays.<FieldConfiguration>asList(new StandardFieldConfiguration());

        List<FieldConfiguration> fields = new ArrayList<FieldConfiguration>();
        for (Map.Entry<String, Object> entry : elements) {
            JsonObject element = (JsonObject) entry.getValue();

            FieldConfiguration configuration = load(entry.getKey(), null, element, context);
            fields.add(configuration);
        }

        return fields;
    }
}
