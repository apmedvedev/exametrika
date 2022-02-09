/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.module;

/**
 * The {@link IModuleFactory} is a module factory.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IModuleFactory {
    /**
     * Creates an instance of module.
     *
     * @return module instance
     */
    IModule createModule();
}
