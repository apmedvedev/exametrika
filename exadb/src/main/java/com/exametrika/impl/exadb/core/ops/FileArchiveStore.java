/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.ops;

import java.io.File;

import com.exametrika.api.exadb.core.config.schema.FileArchiveStoreSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.spi.exadb.core.IArchiveStore;


/**
 * The {@link FileArchiveStore} is a file-based archive store.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class FileArchiveStore implements IArchiveStore {
    private final File path;

    public FileArchiveStore(FileArchiveStoreSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.path = new File(System.getProperty("com.exametrika.workPath"), configuration.getPath());
    }

    @Override
    public void load(String archiveName, String path) {
        Files.copy(new File(this.path, archiveName + ".zip"), new File(path));
    }

    @Override
    public void save(String archiveName, String path) {
        Files.copy(new File(path), new File(this.path, archiveName + ".zip"));
    }

    @Override
    public void close() {
    }
}
