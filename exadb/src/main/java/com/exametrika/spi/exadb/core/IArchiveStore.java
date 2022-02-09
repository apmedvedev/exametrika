/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core;


/**
 * The {@link IArchiveStore} represents an archive store.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IArchiveStore {

    /**
     * Load contents of specified archive from store into file with given name.
     *
     * @param archiveName name of archive
     * @param path        path to file where contents of archive will be written
     */
    void load(String archiveName, String path);

    /**
     * Saves contents of specified file in store.
     *
     * @param archiveName name of archive
     * @param path        path file with archive contents
     */
    void save(String archiveName, String path);

    /**
     * Closes store.
     */
    void close();
}
