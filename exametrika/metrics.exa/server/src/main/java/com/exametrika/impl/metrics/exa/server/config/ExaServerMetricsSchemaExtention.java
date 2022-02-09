/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.config;

import com.exametrika.api.metrics.exa.server.config.ExaServerMonitorConfiguration;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.config.schema.ModuleSchemaLoader.Parameters;


/**
 * The {@link ExaServerMetricsSchemaExtention} is a helper class that is used to load server exa server metrics schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaServerMetricsSchemaExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        parameters.schemaMappings.put("metrics.exa.server", new Pair(
                "classpath:" + Classes.getResourcePath(ExaServerMonitorConfiguration.class) + "/metrics-exa-server.dbschema", false));
        ExaServerMetricsSchemaLoader processor = new ExaServerMetricsSchemaLoader();
        parameters.typeLoaders.put("ExaServerSelector", processor);
        parameters.typeLoaders.put("ExaAgentSelector", processor);
        parameters.typeLoaders.put("AllExaAgentsSelector", processor);
        parameters.typeLoaders.put("ExaAgentDiscoveryStrategy", processor);
        parameters.typeLoaders.put("ExaServerDiscoveryStrategy", processor);
        parameters.typeLoaders.put("exaAgent", processor);
        parameters.typeLoaders.put("exaServer", processor);
        return parameters;
    }
}
