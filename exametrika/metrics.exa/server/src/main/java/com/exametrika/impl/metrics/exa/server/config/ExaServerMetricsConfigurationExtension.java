/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.config;

import com.exametrika.api.metrics.exa.server.config.ExaServerMonitorConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link ExaServerMetricsConfigurationExtension} is a configuration loader extension of Exa metrics.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaServerMetricsConfigurationExtension implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        parameters.schemaMappings.put("metrics.exa.server", new Pair(
                "classpath:" + Classes.getResourcePath(ExaServerMonitorConfiguration.class) + "/metrics-exa-server.schema", false));
        ExaServerMetricsConfigurationLoader processor = new ExaServerMetricsConfigurationLoader();
        parameters.typeLoaders.put("ExaServerMonitor", processor);
        parameters.typeLoaders.put("ExaAggregatorProbe", processor);
        parameters.typeLoaders.put("ExaServerSelector", processor);
        return parameters;
    }
}
