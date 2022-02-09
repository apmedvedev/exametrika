/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import com.exametrika.impl.component.fields.VersionChangesRecordIndexer;
import com.exametrika.spi.exadb.objectdb.config.schema.RecordIndexerSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexProvider;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexer;


/**
 * The {@link VersionChangesRecordIndexerSchemaConfiguration} represents a configuration of schema of version change record indexer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class VersionChangesRecordIndexerSchemaConfiguration extends RecordIndexerSchemaConfiguration {
    @Override
    public IRecordIndexer createIndexer(IField field, IRecordIndexProvider indexProvider) {
        return new VersionChangesRecordIndexer(indexProvider);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof VersionChangesRecordIndexerSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
