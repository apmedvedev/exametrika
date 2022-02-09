/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.config;

import com.exametrika.api.metrics.exa.config.ExaAgentMonitorConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link ExaMetricsConfigurationExtension} is a configuration loader extension of Exa metrics.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaMetricsConfigurationExtension implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        parameters.schemaMappings.put("metrics.exa", new Pair(
                "classpath:" + Classes.getResourcePath(ExaAgentMonitorConfiguration.class) + "/metrics-exa.schema", false));
        ExaMetricsConfigurationLoader processor = new ExaMetricsConfigurationLoader();
        parameters.typeLoaders.put("ExaAgentMonitor", processor);
        parameters.typeLoaders.put("ExaProfilerMonitor", processor);
        parameters.typeLoaders.put("ExaLogProbe", processor);
        parameters.typeLoaders.put("ExaMessagingProbe", processor);
        parameters.typeLoaders.put("ExaInstrumentProbe", processor);
        return parameters;
    }
}
