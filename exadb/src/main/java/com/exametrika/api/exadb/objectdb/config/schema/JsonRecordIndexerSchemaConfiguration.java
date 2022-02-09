/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import com.exametrika.impl.exadb.objectdb.fields.JsonRecordIndexer;
import com.exametrika.spi.exadb.objectdb.config.schema.RecordIndexerSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexProvider;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexer;


/**
 * The {@link JsonRecordIndexerSchemaConfiguration} represents a configuration of schema of json record indexer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JsonRecordIndexerSchemaConfiguration extends RecordIndexerSchemaConfiguration {
    @Override
    public IRecordIndexer createIndexer(IField field, IRecordIndexProvider indexProvider) {
        return new JsonRecordIndexer(field, indexProvider);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JsonRecordIndexerSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}