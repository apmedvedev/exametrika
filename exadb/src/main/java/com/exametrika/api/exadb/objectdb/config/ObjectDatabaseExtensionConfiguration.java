/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config;

import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.DatabaseExtensionConfiguration;

/**
 * The {@link ObjectDatabaseExtensionConfiguration} represents a configuration of object database extension.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectDatabaseExtensionConfiguration extends DatabaseExtensionConfiguration {
    public static final String SCHEMA = "com.exametrika.exadb.objectdb-1.0";
    public static final String NAME = "objectdb";
    private final int maxFreeNodeCacheSize;
    private final long maxFreeNodeIdlePeriod;

    public ObjectDatabaseExtensionConfiguration(int maxFreeNodeCacheSize, long maxFreeNodeIdlePeriod) {
        super(NAME);

        this.maxFreeNodeCacheSize = maxFreeNodeCacheSize;
        this.maxFreeNodeIdlePeriod = maxFreeNodeIdlePeriod;
    }

    public int getMaxFreeNodeCacheSize() {
        return maxFreeNodeCacheSize;
    }

    public long getMaxFreeNodeIdlePeriod() {
        return maxFreeNodeIdlePeriod;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ObjectDatabaseExtensionConfiguration))
            return false;

        ObjectDatabaseExtensionConfiguration configuration = (ObjectDatabaseExtensionConfiguration) o;
        return super.equals(configuration) && maxFreeNodeCacheSize == configuration.maxFreeNodeCacheSize &&
                maxFreeNodeIdlePeriod == configuration.maxFreeNodeIdlePeriod;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(maxFreeNodeCacheSize, maxFreeNodeIdlePeriod);
    }
}
