/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core;

import com.exametrika.common.rawdb.IRawTransaction;


/**
 * The {@link IDataFileAllocator} represents an allocator of data files.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IDataFileAllocator {
    /**
     * Allocates new data files.
     *
     * @param transaction transaction
     * @return index of data file
     */
    int allocateFile(IRawTransaction transaction);
}
