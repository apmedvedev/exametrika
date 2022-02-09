/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Objects;


/**
 * The {@link GcFilterConfiguration} is a configuration of garbage collection filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class GcFilterConfiguration extends Configuration {
    private final long minDuration;
    private final long minBytes;

    public GcFilterConfiguration(long minDuration, long minBytes) {
        this.minDuration = minDuration;
        this.minBytes = minBytes;
    }

    public long getMinDuration() {
        return minDuration;
    }

    public long getMinBytes() {
        return minBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof GcFilterConfiguration))
            return false;

        GcFilterConfiguration configuration = (GcFilterConfiguration) o;
        return minDuration == configuration.minDuration && minBytes == configuration.minBytes;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(minDuration, minBytes);
    }
}
