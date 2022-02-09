/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.config;

import com.exametrika.api.metrics.host.config.HostCpuMonitorConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link HostMetricsConfigurationExtension} is a configuration loader extension of host metrics.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostMetricsConfigurationExtension implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        parameters.schemaMappings.put("metrics.host", new Pair(
                "classpath:" + Classes.getResourcePath(HostCpuMonitorConfiguration.class) + "/metrics-host.schema", false));
        HostMetricsConfigurationLoader processor = new HostMetricsConfigurationLoader();
        parameters.typeLoaders.put("HostCpuMonitor", processor);
        parameters.typeLoaders.put("HostFileSystemMonitor", processor);
        parameters.typeLoaders.put("HostMemoryMonitor", processor);
        parameters.typeLoaders.put("HostNetworkMonitor", processor);
        parameters.typeLoaders.put("HostProcessMonitor", processor);
        parameters.typeLoaders.put("HostCurrentProcessMonitor", processor);
        parameters.typeLoaders.put("HostSwapMonitor", processor);
        parameters.typeLoaders.put("HostKpiMonitor", processor);
        return parameters;
    }
}
