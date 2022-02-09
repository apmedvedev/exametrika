/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;


/**
 * The {@link ICompartmentTaskSize} is a helper interface of task used for estimating task size.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ICompartmentTaskSize {
    /**
     * Returns task size.
     *
     * @return task size
     */
    int getSize();
}
