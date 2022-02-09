/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import java.util.Set;


/**
 * The {@link INavigationAccessorFactory} represents a factory for navigation accessors, i.e. accessors which get values
 * from other nodes of measurement graph.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface INavigationAccessorFactory {
    /**
     * Returns set of supported navigation types.
     *
     * @return set of supported navigation types
     */
    Set<String> getTypes();

    /**
     * Creates accessor.
     *
     * @param navigationType type of navigation accessor
     * @param navigationArgs navigation accessor arguments
     * @param localAccessor  base local accessor
     * @return navigation field accessor
     */
    IComponentAccessor createAccessor(String navigationType, String navigationArgs, IComponentAccessor localAccessor);
}
