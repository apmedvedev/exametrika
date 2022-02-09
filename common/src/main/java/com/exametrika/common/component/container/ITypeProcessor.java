/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.container;

/**
 * The {@link ITypeProcessor} is used to post process component types.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITypeProcessor {
    /**
     * Processes a component type.
     *
     * @param componentType input component type
     * @return output component type
     */
    Object processType(Object componentType);
}
