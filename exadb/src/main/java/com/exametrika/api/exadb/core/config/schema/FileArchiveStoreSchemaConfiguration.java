/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.config.schema;

import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.ops.FileArchiveStore;
import com.exametrika.spi.exadb.core.IArchiveStore;
import com.exametrika.spi.exadb.core.config.schema.ArchiveStoreSchemaConfiguration;


/**
 * The {@link FileArchiveStoreSchemaConfiguration} is a file-based archive store configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class FileArchiveStoreSchemaConfiguration extends ArchiveStoreSchemaConfiguration {
    private final String path;

    public FileArchiveStoreSchemaConfiguration(String path) {
        Assert.notNull(path);

        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public IArchiveStore createStore() {
        return new FileArchiveStore(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FileArchiveStoreSchemaConfiguration))
            return false;

        FileArchiveStoreSchemaConfiguration configuration = (FileArchiveStoreSchemaConfiguration) o;
        return path.equals(configuration.path);
    }

    @Override
    public int hashCode() {
        return 31 * path.hashCode();
    }

    @Override
    public String toString() {
        return path;
    }
}
