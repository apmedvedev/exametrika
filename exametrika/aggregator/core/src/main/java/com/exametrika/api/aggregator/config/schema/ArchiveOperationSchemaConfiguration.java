/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.ops.ArchiveOperation;
import com.exametrika.spi.aggregator.config.schema.ArchivePolicySchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.ArchiveStoreSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.IJobContext;
import com.exametrika.spi.exadb.jobs.config.model.JobOperationSchemaConfiguration;


/**
 * The {@link ArchiveOperationSchemaConfiguration} is a period cycles archiving operation configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ArchiveOperationSchemaConfiguration extends JobOperationSchemaConfiguration {
    private final NameFilter spaceFilter;
    private final List<String> periods;
    private final ArchivePolicySchemaConfiguration archivePolicy;
    private final ArchiveStoreSchemaConfiguration archiveStore;

    public ArchiveOperationSchemaConfiguration(NameFilter spaceFilter, List<String> periods, ArchivePolicySchemaConfiguration archivePolicy,
                                               ArchiveStoreSchemaConfiguration archiveStore) {
        Assert.notNull(archivePolicy);
        Assert.notNull(archiveStore);

        this.spaceFilter = spaceFilter;
        this.periods = Immutables.wrap(periods);
        this.archivePolicy = archivePolicy;
        this.archiveStore = archiveStore;
    }

    public NameFilter getSpaceFilter() {
        return spaceFilter;
    }

    public List<String> getPeriods() {
        return periods;
    }

    public ArchivePolicySchemaConfiguration getArchivePolicy() {
        return archivePolicy;
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
        return new ArchiveOperation(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ArchiveOperationSchemaConfiguration))
            return false;

        ArchiveOperationSchemaConfiguration configuration = (ArchiveOperationSchemaConfiguration) o;
        return Objects.equals(spaceFilter, configuration.spaceFilter) && Objects.equals(periods, configuration.periods) &&
                archivePolicy.equals(configuration.archivePolicy) && archiveStore.equals(configuration.archiveStore);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(spaceFilter, periods, archivePolicy, archiveStore);
    }
}
