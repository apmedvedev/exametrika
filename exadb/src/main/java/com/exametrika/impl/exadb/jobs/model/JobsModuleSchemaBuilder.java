/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.model;

import java.util.Map;

import com.exametrika.api.exadb.core.config.schema.DatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleDependencySchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.schema.JobExecutionNodeSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.schema.JobNodeSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.schema.JobRootNodeSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.schema.JobServiceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Version;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SpaceSchemaConfiguration;


/**
 * The {@link JobsModuleSchemaBuilder} is a schema builder for jobs module.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class JobsModuleSchemaBuilder {
    public ModuleSchemaConfiguration createRequiredModule() {
        return new ModuleSchemaConfiguration("exa.Jobs", new Version(1, 0, 0),
                new DatabaseSchemaConfiguration("exa.Jobs", Collections.asSet(new DomainSchemaConfiguration("system",
                        Collections.asSet(new ObjectSpaceSchemaConfiguration("jobs",
                                Collections.asSet(new JobRootNodeSchemaConfiguration(), new JobNodeSchemaConfiguration(),
                                        new JobExecutionNodeSchemaConfiguration()), "Root")),
                        java.util.Collections.<DomainServiceSchemaConfiguration>singleton(new JobServiceSchemaConfiguration(
                                java.util.Collections.<String, JobSchemaConfiguration>emptyMap()))))),
                java.util.Collections.<ModuleDependencySchemaConfiguration>emptySet());
    }

    public ModuleSchemaConfiguration createModule(String name, Version version, Map<String, JobSchemaConfiguration> predefinedJobs) {
        return new ModuleSchemaConfiguration(name, version,
                new DatabaseSchemaConfiguration(name, Collections.asSet(new DomainSchemaConfiguration("system",
                        Collections.<SpaceSchemaConfiguration>asSet(),
                        java.util.Collections.<DomainServiceSchemaConfiguration>singleton(new JobServiceSchemaConfiguration(predefinedJobs))))),
                Collections.asSet(new ModuleDependencySchemaConfiguration("exa.Jobs", new Version(1, 0, 0))));
    }
}