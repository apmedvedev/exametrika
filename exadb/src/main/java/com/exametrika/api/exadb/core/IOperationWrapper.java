/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core;


/**
 * The {@link IOperationWrapper} represents an operation wrapper.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IOperationWrapper {
    /**
     * Returns operation.
     *
     * @param <T> operation type
     * @return operation
     */
    <T> T getOperation();
}
