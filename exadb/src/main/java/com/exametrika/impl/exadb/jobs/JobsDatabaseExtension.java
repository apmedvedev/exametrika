/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs;

import java.util.Collections;
import java.util.Set;

import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.impl.exadb.core.AbstractDatabaseExtension;
import com.exametrika.impl.exadb.jobs.model.JobsModuleSchemaBuilder;


/**
 * The {@link JobsDatabaseExtension} represents a jobs database extension.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobsDatabaseExtension extends AbstractDatabaseExtension {
    public JobsDatabaseExtension() {
        super("jobs");
    }

    @Override
    public Set<ModuleSchemaConfiguration> getRequiredModules() {
        return Collections.singleton(new JobsModuleSchemaBuilder().createRequiredModule());
    }
}
