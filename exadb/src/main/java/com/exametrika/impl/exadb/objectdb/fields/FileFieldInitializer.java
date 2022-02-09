/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.fields.IFileFieldInitializer;


/**
 * The {@link FileFieldInitializer} is an initializer of file field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class FileFieldInitializer implements IFileFieldInitializer {
    private int pathIndex = -1;
    private long maxFileSize;
    private String directory;

    public int getPathIndex() {
        return pathIndex;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public String getDirectory() {
        return directory;
    }

    @Override
    public void setPathIndex(int pathIndex) {
        this.pathIndex = pathIndex;
    }

    @Override
    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    @Override
    public void setDirectory(String directory) {
        this.directory = directory;
    }
}
