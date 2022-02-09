/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.meters.config;

import com.exametrika.impl.aggregator.common.meters.ErrorLogProvider;
import com.exametrika.spi.aggregator.common.meters.ILogProvider;
import com.exametrika.spi.aggregator.common.meters.config.LogProviderConfiguration;


/**
 * The {@link ErrorLogProviderConfiguration} is a log event error log provider configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ErrorLogProviderConfiguration extends LogProviderConfiguration {
    @Override
    public ILogProvider createProvider() {
        return new ErrorLogProvider();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ErrorLogProviderConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "errorLog";
    }
}
