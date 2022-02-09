/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;


/**
 * The {@link ICacheable} represents an interface to get cache size of object.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ICacheable {
    /**
     * Returns cache size of object.
     *
     * @return cache size of object
     */
    int getCacheSize();
}
