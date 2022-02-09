/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource;


/**
 * The {@link IResourceProvider} represents a provider of resource.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IResourceProvider {
    /**
     * Returns amount of resource which provider has.
     *
     * @return amount of resource provider has
     */
    long getAmount();
}
