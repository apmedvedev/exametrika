/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.container;

/**
 * The {@link IComponentFactory} is a component factory.
 *
 * @param <T> type name of component
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IComponentFactory<T> {
    /**
     * Creates an instance of component.
     *
     * @return component instance
     */
    T createComponent();
}
