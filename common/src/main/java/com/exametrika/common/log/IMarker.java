/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log;


/**
 * The {@link IMarker} represents a logging marker.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMarker {
    /**
     * Returns marker name.
     *
     * @return marker name
     */
    String getName();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
