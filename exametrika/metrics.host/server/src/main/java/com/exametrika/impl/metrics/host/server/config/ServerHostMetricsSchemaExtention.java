/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.server.config;

import com.exametrika.api.metrics.host.server.config.model.HostErrorsSchemaConfiguration;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.config.schema.ModuleSchemaLoader.Parameters;


/**
 * The {@link ServerHostMetricsSchemaExtention} is a helper class that is used to load server host metrics schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ServerHostMetricsSchemaExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        ServerHostMetricsSchemaLoader processor = new ServerHostMetricsSchemaLoader();
        parameters.typeLoaders.put("HostWorkloadRepresentation", processor);
        parameters.typeLoaders.put("HostErrorsRepresentation", processor);
        parameters.typeLoaders.put("HostWorkloadMetric", processor);
        parameters.typeLoaders.put("HostErrorsMetric", processor);
        parameters.typeLoaders.put("AllHostsSelector", processor);
        parameters.typeLoaders.put("HostSelector", processor);
        parameters.schemaMappings.put(HostErrorsSchemaConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(HostErrorsSchemaConfiguration.class) + "/metrics-host-server.dbschema", false));
        return parameters;
    }
}
