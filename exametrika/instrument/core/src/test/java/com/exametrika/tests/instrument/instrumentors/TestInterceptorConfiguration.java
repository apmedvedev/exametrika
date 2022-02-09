/**
 * Copyright 2012 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import com.exametrika.spi.instrument.config.DynamicInterceptorConfiguration;
import com.exametrika.spi.instrument.intercept.IDynamicInterceptor;

public class TestInterceptorConfiguration extends DynamicInterceptorConfiguration {
    @Override
    public IDynamicInterceptor createInterceptor() {
        return new InterceptorMock();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TestInterceptorConfiguration))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
