/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.factory.external;

import com.exametrika.common.component.container.IComponentFactory;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ExternalComponentFactory} is a component factory based on external component.
 *
 * @param <T> type name of component
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExternalComponentFactory<T> implements IComponentFactory<T> {
    private final T component;

    /**
     * Creates a new object.
     *
     * @param component external component
     */
    public ExternalComponentFactory(T component) {
        Assert.notNull(component);

        this.component = component;
    }

    @Override
    public T createComponent() {
        return component;
    }
}
