/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.config;

import com.exametrika.api.metrics.exa.config.ExaAgentMonitorConfiguration;
import com.exametrika.api.metrics.exa.config.ExaInstrumentProbeConfiguration;
import com.exametrika.api.metrics.exa.config.ExaLogProbeConfiguration;
import com.exametrika.api.metrics.exa.config.ExaMessagingProbeConfiguration;
import com.exametrika.api.metrics.exa.config.ExaProfilerMonitorConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonObject;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;

/**
 * The {@link ExaMetricsConfigurationLoader} is a configuration loader of Exa metrics.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExaMetricsConfigurationLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;

        if (type.equals("ExaAgentMonitor")) {
            String scope = element.get("scope", null);
            long period = element.get("period");
            String measurementStrategy = element.get("measurementStrategy", null);

            return new ExaAgentMonitorConfiguration(name, scope, period, measurementStrategy);
        } else if (type.equals("ExaProfilerMonitor")) {
            String scope = element.get("scope", null);
            long period = element.get("period");
            String measurementStrategy = element.get("measurementStrategy", null);

            return new ExaProfilerMonitorConfiguration(name, scope, period, measurementStrategy);
        } else if (type.equals("ExaLogProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long extractionPeriod = element.get("extractionPeriod");
            long warmupDelay = element.get("warmupDelay");
            LogConfiguration log = load(null, "Log", (JsonObject) element.get("log"), context);

            return new ExaLogProbeConfiguration(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay, log);
        } else if (type.equals("ExaMessagingProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long extractionPeriod = element.get("extractionPeriod");
            long warmupDelay = element.get("warmupDelay");

            return new ExaMessagingProbeConfiguration(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay);
        } else if (type.equals("ExaInstrumentProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long extractionPeriod = element.get("extractionPeriod");
            long warmupDelay = element.get("warmupDelay");

            return new ExaInstrumentProbeConfiguration(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay);
        } else
            throw new InvalidConfigurationException();
    }
}