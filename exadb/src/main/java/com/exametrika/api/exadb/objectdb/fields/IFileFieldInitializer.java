/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;


/**
 * The {@link IFileFieldInitializer} represents an initializer of file field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IFileFieldInitializer {
    /**
     * Sets index of database main path where file resides.
     *
     * @param pathIndex index of database main path where file resides
     */
    void setPathIndex(int pathIndex);

    /**
     * Maximum size of file. If max file size is 0 then default database max file size is used.
     *
     * @param maxFileSize maximum size of file
     */
    void setMaxFileSize(long maxFileSize);

    /**
     * Sets name of directory where file resides. Directory name is relative to space's files directory.
     *
     * @param directory file's relative directory name
     */
    void setDirectory(String directory);
}
