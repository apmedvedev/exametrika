/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.config.schema;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexProvider;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexer;


/**
 * The {@link RecordIndexerSchemaConfiguration} represents a configuration of record indexer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class RecordIndexerSchemaConfiguration extends Configuration {
    public abstract IRecordIndexer createIndexer(IField field, IRecordIndexProvider indexProvider);
}
