/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;


/**
 * The {@link ITransactionInfo} represents an information about current transaction.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITransactionInfo {
    /**
     * Returns combine stack identifier.
     *
     * @return combine stack identifier
     */
    String getCombineId();

    /**
     * Returns identifier of transaction.
     *
     * @return identifier of transaction
     */
    long getId();

    /**
     * Returns transaction start time.
     *
     * @return transaction start time
     */
    long getStartTime();
}
