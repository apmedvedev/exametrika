/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.model;


/**
 * The {@link IName} represents a measurement name.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IName {
    /**
     * Is name empty?
     *
     * @return true if name is empty
     */
    boolean isEmpty();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
