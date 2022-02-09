/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link CacheCategoryTypeConfiguration} is a configuration of cache category type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CacheCategoryTypeConfiguration extends Configuration {
    private final String name;
    private final long initialCacheSize;
    private final double minCachePercentage;
    private final long maxCacheIdlePeriod;

    public CacheCategoryTypeConfiguration(String name, long initialCacheSize, double minCachePercentage, long maxIdlePeriod) {
        Assert.notNull(name);
        Assert.isTrue(minCachePercentage >= 90 && minCachePercentage <= 100);

        this.name = name;
        this.initialCacheSize = initialCacheSize;
        this.minCachePercentage = minCachePercentage;
        this.maxCacheIdlePeriod = maxIdlePeriod;
    }

    public String getName() {
        return name;
    }

    public long getInitialCacheSize() {
        return initialCacheSize;
    }

    public double getMinCachePercentage() {
        return minCachePercentage;
    }

    public long getMaxIdlePeriod() {
        return maxCacheIdlePeriod;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CacheCategoryTypeConfiguration))
            return false;

        CacheCategoryTypeConfiguration configuration = (CacheCategoryTypeConfiguration) o;
        return name.equals(configuration.name) && initialCacheSize == configuration.initialCacheSize &&
                minCachePercentage == configuration.minCachePercentage &&
                maxCacheIdlePeriod == configuration.maxCacheIdlePeriod;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, initialCacheSize, minCachePercentage, maxCacheIdlePeriod);
    }

    @Override
    public String toString() {
        return name.isEmpty() ? "<default>" : name;
    }
}
