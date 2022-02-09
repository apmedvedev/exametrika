/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core;

import java.util.List;


/**
 * The {@link IExtensionSpace} represents a extension space.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IExtensionSpace {
    /**
     * Returns space files.
     *
     * @return space files
     */
    List<String> getFiles();

    /**
     * Returns initialization priority. Spaces with lower priority are initialized first.
     *
     * @return initialization priority of space
     */
    int getPriority();

    /**
     * Creates space in transaction.
     */
    void create();

    /**
     * Opens space in transaction.
     */
    void open();
}
