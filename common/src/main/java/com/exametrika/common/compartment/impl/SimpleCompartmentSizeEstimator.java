/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.compartment.impl;

import com.exametrika.common.compartment.ICompartmentSizeEstimator;
import com.exametrika.common.compartment.ICompartmentTaskSize;


/**
 * The {@link SimpleCompartmentSizeEstimator} is a simple compartment size estimator.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SimpleCompartmentSizeEstimator implements ICompartmentSizeEstimator {
    @Override
    public int estimateSize(Object value) {
        if (value instanceof ICompartmentTaskSize)
            return ((ICompartmentTaskSize) value).getSize();
        else
            return 1;
    }
}
