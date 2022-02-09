/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.model;


/**
 * The {@link IMeasurementId} represents a measurement identifier.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IMeasurementId {
    /**
     * Returns component type.
     *
     * @return component type
     */
    String getComponentType();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
