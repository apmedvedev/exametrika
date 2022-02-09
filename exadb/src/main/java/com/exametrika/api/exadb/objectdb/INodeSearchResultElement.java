/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb;


/**
 * The {@link INodeSearchResultElement} represents a single element of result of query search.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface INodeSearchResultElement {
    /**
     * Returns value.
     *
     * @return value
     */
    <T> T get();
}
