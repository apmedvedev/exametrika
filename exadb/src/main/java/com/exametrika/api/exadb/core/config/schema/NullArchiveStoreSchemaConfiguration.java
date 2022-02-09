/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.config.schema;

import com.exametrika.impl.exadb.core.ops.NullArchiveStore;
import com.exametrika.spi.exadb.core.IArchiveStore;
import com.exametrika.spi.exadb.core.config.schema.ArchiveStoreSchemaConfiguration;


/**
 * The {@link NullArchiveStoreSchemaConfiguration} is a null (no-op) archive store configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NullArchiveStoreSchemaConfiguration extends ArchiveStoreSchemaConfiguration {
    @Override
    public IArchiveStore createStore() {
        return new NullArchiveStore();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NullArchiveStoreSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return 31 * getClass().hashCode();
    }

    @Override
    public String toString() {
        return "null";
    }
}
