/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.ops;

import com.exametrika.spi.exadb.core.IArchiveStore;


/**
 * The {@link NullArchiveStore} is a null (no-op) archive store.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NullArchiveStore implements IArchiveStore {
    @Override
    public void load(String archiveName, String path) {
    }

    @Override
    public void save(String archiveName, String path) {
    }

    @Override
    public void close() {
    }
}
