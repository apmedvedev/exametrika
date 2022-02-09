/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;


/**
 * The {@link IMetricAccessorFactory} represents an metric accessor factory.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMetricAccessorFactory {
    /**
     * Returns metric index.
     *
     * @return metric index
     */
    int getMetricIndex();

    /**
     * Returns component accessor factory.
     *
     * @return component accessor factory
     */
    IComponentAccessorFactory getComponentAccessorFactory();

    /**
     * Sets component accessor factory.
     *
     * @param componentAccessorFactory component accessor factory
     */
    void setComponentAccessorFactory(IComponentAccessorFactory componentAccessorFactory);

    /**
     * Creates metric accessor.
     *
     * @param navigationType type of navigation accessor or null if accessor is local
     * @param navigationArgs navigation accessor arguments
     * @param fieldName      field name (excluding metric name)
     * @return field accessor
     */
    IMetricAccessor createAccessor(String navigationType, String navigationArgs, String fieldName);
}
