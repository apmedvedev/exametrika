/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.jvm;


/**
 * The {@link IFileRawRequest} represents a file raw request.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IFileRawRequest {
    /**
     * Returns file path.
     *
     * @return file path
     */
    String getPath();

    /**
     * Is read or write operation performed?
     *
     * @return true if read operation is performed
     */
    boolean isRead();
}
