/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.config.schema;

import com.exametrika.api.exadb.jobs.config.JobServiceConfiguration;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.schema.JobServiceSchemaConfiguration;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.config.schema.ModuleSchemaLoader.Parameters;


/**
 * The {@link JobsSchemaExtention} is a helper class that is used to load {@link JobServiceConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobsSchemaExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        IExtensionLoader processor = new JobsSchemaLoader();
        parameters.topLevelDatabaseElements.put("predefinedJobs", new Pair<String, Boolean>("PredefinedJobs", false));
        parameters.typeLoaders.put("JobService", processor);
        parameters.typeLoaders.put("PredefinedJobs", processor);
        parameters.typeLoaders.put("Jobs", processor);
        parameters.typeLoaders.put("StandardSchedule", processor);
        parameters.typeLoaders.put("StandardSchedulePeriod", processor);
        parameters.schemaMappings.put(JobServiceSchemaConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(JobSchemaConfiguration.class) + "/jobs.dbschema", false));
        return parameters;
    }
}
