/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.config;

import com.exametrika.api.metrics.exa.server.config.ExaAggregatorProbeConfiguration;
import com.exametrika.api.metrics.exa.server.config.ExaServerMonitorConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonObject;

/**
 * The {@link ExaServerMetricsConfigurationLoader} is a configuration loader of Exa metrics.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExaServerMetricsConfigurationLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;

        if (type.equals("ExaServerMonitor")) {
            String scope = element.get("scope", null);
            long period = element.get("period");
            String measurementStrategy = element.get("measurementStrategy", null);

            return new ExaServerMonitorConfiguration(name, scope, period, measurementStrategy);
        } else if (type.equals("ExaAggregatorProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long extractionPeriod = element.get("extractionPeriod");
            long warmupDelay = element.get("warmupDelay");

            return new ExaAggregatorProbeConfiguration(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay);
        } else
            throw new InvalidConfigurationException();
    }
}