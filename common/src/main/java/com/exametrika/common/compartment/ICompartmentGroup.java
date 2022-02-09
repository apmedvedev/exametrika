/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;

import com.exametrika.common.utils.ILifecycle;


/**
 * The {@link ICompartmentGroup} is a compartment group which provides thread pool and timer to compartments belonging
 * to this group.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ICompartmentGroup extends ICompartmentGroupMXBean, ILifecycle {
}
