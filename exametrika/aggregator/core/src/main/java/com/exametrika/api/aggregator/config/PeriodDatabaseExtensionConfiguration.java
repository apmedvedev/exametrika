/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config;

import com.exametrika.spi.exadb.core.config.DatabaseExtensionConfiguration;

/**
 * The {@link PeriodDatabaseExtensionConfiguration} represents a configuration of period database extension.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodDatabaseExtensionConfiguration extends DatabaseExtensionConfiguration {
    public static final String SCHEMA = "com.exametrika.exadb.perfdb-1.0";
    public static final String NAME = "perfdb";

    public PeriodDatabaseExtensionConfiguration() {
        super(NAME);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PeriodDatabaseExtensionConfiguration))
            return false;

        PeriodDatabaseExtensionConfiguration configuration = (PeriodDatabaseExtensionConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
