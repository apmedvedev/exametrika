/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.module;

import com.exametrika.common.component.container.IComponentContainer;

/**
 * The {@link IModule} is a module, i.e. unit of deployment of component application.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IModule {
    /**
     * Returns module name.
     *
     * @return module name
     */
    String getName();

    /**
     * Returns module's component container.
     *
     * @return module's component container
     */
    IComponentContainer getContainer();
}
