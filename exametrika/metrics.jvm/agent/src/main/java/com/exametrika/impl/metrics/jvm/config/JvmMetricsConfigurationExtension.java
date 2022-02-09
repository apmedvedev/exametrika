/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.config;

import com.exametrika.api.metrics.jvm.config.JvmKpiMonitorConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link JvmMetricsConfigurationExtension} is a configuration loader extension of JVM metrics.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JvmMetricsConfigurationExtension implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        parameters.schemaMappings.put("metrics.jvm", new Pair(
                "classpath:" + Classes.getResourcePath(JvmKpiMonitorConfiguration.class) + "/metrics-jvm.schema", false));
        JvmMetricsConfigurationLoader processor = new JvmMetricsConfigurationLoader();
        parameters.typeLoaders.put("JvmBufferPoolMonitor", processor);
        parameters.typeLoaders.put("JvmCodeMonitor", processor);
        parameters.typeLoaders.put("JvmRuntimeMonitor", processor);
        parameters.typeLoaders.put("JvmKpiMonitor", processor);
        parameters.typeLoaders.put("JvmMemoryMonitor", processor);
        parameters.typeLoaders.put("JvmSunMemoryMonitor", processor);
        parameters.typeLoaders.put("JvmThreadMonitor", processor);
        parameters.typeLoaders.put("JvmSunThreadMonitor", processor);
        parameters.typeLoaders.put("JmxMonitor", processor);
        parameters.typeLoaders.put("JulProbe", processor);
        parameters.typeLoaders.put("Log4jProbe", processor);
        parameters.typeLoaders.put("LogbackProbe", processor);
        parameters.typeLoaders.put("JmsConsumerProbe", processor);
        parameters.typeLoaders.put("JmsProducerProbe", processor);
        parameters.typeLoaders.put("FileProbe", processor);
        parameters.typeLoaders.put("TcpProbe", processor);
        parameters.typeLoaders.put("UdpProbe", processor);
        parameters.typeLoaders.put("JdbcProbe", processor);
        parameters.typeLoaders.put("JdbcConnectionProbe", processor);
        parameters.typeLoaders.put("HttpConnectionProbe", processor);
        parameters.typeLoaders.put("HttpServletProbe", processor);
        parameters.typeLoaders.put("UrlRequestGroupingStrategy", processor);
        parameters.typeLoaders.put("JdbcRequestGroupingStrategy", processor);
        parameters.validators.put("jmxObjectName", new JmxObjectNameValidator());
        return parameters;
    }
}
