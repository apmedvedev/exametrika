/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core.config.schema;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.exadb.core.IArchiveStore;

/**
 * The {@link ArchiveStoreSchemaConfiguration} represents a configuration of archive store for archived space data files.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ArchiveStoreSchemaConfiguration extends Configuration {
    public abstract IArchiveStore createStore();
}
