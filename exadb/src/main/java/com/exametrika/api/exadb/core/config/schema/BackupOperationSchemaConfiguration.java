/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.config.schema;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.ops.BackupOperation;
import com.exametrika.spi.exadb.core.config.schema.ArchiveStoreSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.IJobContext;
import com.exametrika.spi.exadb.jobs.config.model.JobOperationSchemaConfiguration;


/**
 * The {@link BackupOperationSchemaConfiguration} is a backup operation configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BackupOperationSchemaConfiguration extends JobOperationSchemaConfiguration {
    private final ArchiveStoreSchemaConfiguration archiveStore;

    public BackupOperationSchemaConfiguration(ArchiveStoreSchemaConfiguration archiveStore) {
        Assert.notNull(archiveStore);

        this.archiveStore = archiveStore;
    }

    public ArchiveStoreSchemaConfiguration getArchiveStore() {
        return archiveStore;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public Runnable createOperation(IJobContext context) {
        return new BackupOperation(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BackupOperationSchemaConfiguration))
            return false;

        BackupOperationSchemaConfiguration configuration = (BackupOperationSchemaConfiguration) o;
        return archiveStore.equals(configuration.archiveStore);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(archiveStore);
    }
}
