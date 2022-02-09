/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.config;

import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.jobs.config.JobServiceConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link JobsConfigurationExtention} is a helper class that is used to load {@link DatabaseConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobsConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        IExtensionLoader processor = new JobsConfigurationLoader();
        parameters.typeLoaders.put("JobService", processor);
        parameters.schemaMappings.put(JobServiceConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(JobServiceConfiguration.class) + "/jobs.schema", false));
        return parameters;
    }
}
