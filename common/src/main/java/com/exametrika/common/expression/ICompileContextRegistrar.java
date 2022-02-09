/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression;


/**
 * The {@link ICompileContextRegistrar} is a helper object used to initialize compile context from service registry.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ICompileContextRegistrar {
    /**
     * Registers neccessary imports or other elements in specified compile context.
     *
     * @param context compile context
     */
    void register(CompileContext context);
}
