/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;


/**
 * The {@link IComponentAccessorFactory} represents an component accessor factory.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IComponentAccessorFactory {
    /**
     * Does component have metric with specified name.
     *
     * @param fieldName field name
     * @return true if field name starts with existing metric name
     */
    boolean hasMetric(String fieldName);

    /**
     * Creates accessor.
     *
     * @param navigationType type of navigation accessor or null if accessor is local
     * @param navigationArgs navigation accessor arguments
     * @param fieldName      field name (including metric name)
     * @return component accessor
     */
    IComponentAccessor createAccessor(String navigationType, String navigationArgs, String fieldName);
}
