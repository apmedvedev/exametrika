/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.meters;

import com.exametrika.spi.aggregator.common.meters.ILogEvent;
import com.exametrika.spi.aggregator.common.meters.ILogProvider;


/**
 * The {@link CountLogProvider} is count log provider implementation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class CountLogProvider implements ILogProvider {
    @Override
    public Object getValue(ILogEvent value) {
        return 1;
    }
}
