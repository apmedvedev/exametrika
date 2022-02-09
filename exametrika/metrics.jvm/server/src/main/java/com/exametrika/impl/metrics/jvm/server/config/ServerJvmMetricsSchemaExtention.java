/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.config;

import com.exametrika.api.metrics.jvm.server.config.model.JvmErrorsSchemaConfiguration;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.config.schema.ModuleSchemaLoader.Parameters;


/**
 * The {@link ServerJvmMetricsSchemaExtention} is a helper class that is used to load server jvm metrics schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ServerJvmMetricsSchemaExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        ServerJvmMetricsSchemaLoader processor = new ServerJvmMetricsSchemaLoader();
        parameters.typeLoaders.put("JvmWorkloadRepresentation", processor);
        parameters.typeLoaders.put("JvmErrorsRepresentation", processor);
        parameters.typeLoaders.put("JvmWorkloadMetric", processor);
        parameters.typeLoaders.put("JvmErrorsMetric", processor);
        parameters.typeLoaders.put("AppWorkloadRepresentation", processor);
        parameters.typeLoaders.put("AppErrorsRepresentation", processor);
        parameters.typeLoaders.put("AppWorkloadMetric", processor);
        parameters.typeLoaders.put("AppErrorsMetric", processor);
        parameters.typeLoaders.put("AllJvmNodesSelector", processor);
        parameters.typeLoaders.put("JvmNodeSelector", processor);
        parameters.typeLoaders.put("AllTransactionsSelector", processor);
        parameters.typeLoaders.put("AllHotspotsSelector", processor);
        parameters.typeLoaders.put("TransactionSelector", processor);
        parameters.schemaMappings.put(JvmErrorsSchemaConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(JvmErrorsSchemaConfiguration.class) + "/metrics-jvm-server.dbschema", false));
        return parameters;
    }
}
