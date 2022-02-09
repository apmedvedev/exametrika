/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.meters.config;

import com.exametrika.impl.aggregator.common.meters.ErrorCountLogProvider;
import com.exametrika.spi.aggregator.common.meters.ILogProvider;
import com.exametrika.spi.aggregator.common.meters.config.LogProviderConfiguration;


/**
 * The {@link ErrorCountLogProviderConfiguration} is a log event error count log provider configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ErrorCountLogProviderConfiguration extends LogProviderConfiguration {
    @Override
    public ILogProvider createProvider() {
        return new ErrorCountLogProvider();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ErrorCountLogProviderConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "errorCount";
    }
}
