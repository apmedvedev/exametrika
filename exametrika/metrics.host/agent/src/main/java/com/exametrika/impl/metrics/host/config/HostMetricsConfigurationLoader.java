/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.config;

import java.util.List;
import java.util.Map;

import com.exametrika.api.metrics.host.config.ExpressionProcessNamingStrategyConfiguration;
import com.exametrika.api.metrics.host.config.HostCpuMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostCurrentProcessMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostFileSystemMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostKpiMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostMemoryMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostNetworkMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostProcessMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostSwapMonitorConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.spi.metrics.host.ProcessNamingStrategyConfiguration;

/**
 * The {@link HostMetricsConfigurationLoader} is a configuration loader of host metrics.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HostMetricsConfigurationLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        String scope = element.get("scope", null);
        long period = element.get("period");
        String measurementStrategy = element.get("measurementStrategy", null);

        if (type.equals("HostCpuMonitor"))
            return new HostCpuMonitorConfiguration(name, scope, period, measurementStrategy);
        else if (type.equals("HostFileSystemMonitor")) {
            String filter = element.get("filter", null);
            return new HostFileSystemMonitorConfiguration(name, scope, period, measurementStrategy, filter);
        } else if (type.equals("HostMemoryMonitor"))
            return new HostMemoryMonitorConfiguration(name, scope, period, measurementStrategy);
        else if (type.equals("HostNetworkMonitor")) {
            String filter = element.get("filter", null);
            boolean extendedStatistics = element.get("extendedStatistics");
            boolean tcpStatistics = element.get("tcpStatistics");
            Map<String, Long> networkInterfaceSpeed = JsonUtils.toMap((JsonObject) element.get("networkInterfaceSpeed", null));
            return new HostNetworkMonitorConfiguration(name, scope, period, measurementStrategy, filter,
                    extendedStatistics, tcpStatistics, networkInterfaceSpeed);
        } else if (type.equals("HostProcessMonitor")) {
            List<String> filters = JsonUtils.toList((JsonArray) element.get("filters", null));
            ProcessNamingStrategyConfiguration namingStrategy = loadNamingStrategy(element.get("namingStrategy", null), context);
            return new HostProcessMonitorConfiguration(name, scope, period, measurementStrategy, filters, namingStrategy);
        } else if (type.equals("HostCurrentProcessMonitor")) {
            return new HostCurrentProcessMonitorConfiguration(name, scope, period, measurementStrategy);
        } else if (type.equals("HostSwapMonitor"))
            return new HostSwapMonitorConfiguration(name, scope, period, measurementStrategy);
        else if (type.equals("HostKpiMonitor")) {
            String componentType = element.get("componentType");
            String fileSystemFilter = element.get("fileSystemFilter", null);
            String networkInterfaceFilter = element.get("networkInterfaceFilter", null);
            return new HostKpiMonitorConfiguration(name, scope, period, measurementStrategy, componentType,
                    fileSystemFilter, networkInterfaceFilter);
        } else
            throw new InvalidConfigurationException();
    }

    private ProcessNamingStrategyConfiguration loadNamingStrategy(Object element, ILoadContext context) {
        if (element == null)
            return null;
        else if (element instanceof String)
            return new ExpressionProcessNamingStrategyConfiguration((String) element);
        else
            return load(null, null, (JsonObject) element, context);
    }
}