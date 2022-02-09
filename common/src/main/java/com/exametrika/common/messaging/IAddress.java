/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;

import java.util.UUID;


/**
 * The {@link IAddress} represents a node address. Addresses are compared by their identifiers.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAddress extends Comparable<IAddress> {
    /**
     * Returns address name.
     *
     * @return address name
     */
    String getName();

    /**
     * Returns number of transport addresses in this address.
     *
     * @return number of transport addresses in this address
     */
    int getCount();

    /**
     * Returns canonicalized connection address.
     *
     * @param transportId transport identifier
     * @return canonicalized connection address
     */
    String getConnection(int transportId);

    /**
     * Returns address unique identifier.
     *
     * @return address unique identifier
     */
    UUID getId();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
