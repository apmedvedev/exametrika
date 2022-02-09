/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.index;

import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.spi.exadb.core.IDatabaseExtension;


/**
 * The {@link IIndexDatabaseExtension} represents an index database extension.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IIndexDatabaseExtension extends IDatabaseExtension {
    /**
     * Returns index manager.
     *
     * @return index manager
     */
    IIndexManager getIndexManager();
}
