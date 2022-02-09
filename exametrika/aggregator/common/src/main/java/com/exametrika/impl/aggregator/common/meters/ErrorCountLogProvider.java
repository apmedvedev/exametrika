/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.meters;

import com.exametrika.spi.aggregator.common.meters.ILogEvent;
import com.exametrika.spi.aggregator.common.meters.ILogProvider;


/**
 * The {@link ErrorCountLogProvider} is error count log provider implementation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ErrorCountLogProvider implements ILogProvider {
    @Override
    public Object getValue(ILogEvent value) {
        if (value.isError())
            return 1;
        else
            return null;
    }
}
