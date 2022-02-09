/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.core.config.schema.DatabaseExtensionSchemaConfiguration;

/**
 * The {@link PeriodDatabaseExtensionSchemaConfiguration} represents a configuration of schema of period database extension.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodDatabaseExtensionSchemaConfiguration extends DatabaseExtensionSchemaConfiguration {
    public static final String SCHEMA = "com.exametrika.exadb.perfdb-1.0";
    public static final String NAME = "perfdb";
    private final NameSpaceSchemaConfiguration nameSpace;

    public PeriodDatabaseExtensionSchemaConfiguration() {
        this(new NameSpaceSchemaConfiguration());
    }

    public PeriodDatabaseExtensionSchemaConfiguration(NameSpaceSchemaConfiguration nameSpace) {
        this(NAME, null, nameSpace);
    }

    public PeriodDatabaseExtensionSchemaConfiguration(String alias, String description, NameSpaceSchemaConfiguration nameSpace) {
        super(NAME, alias, description);

        Assert.notNull(nameSpace);

        this.nameSpace = nameSpace;
    }

    public NameSpaceSchemaConfiguration getNameSpace() {
        return nameSpace;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PeriodDatabaseExtensionSchemaConfiguration))
            return false;

        PeriodDatabaseExtensionSchemaConfiguration configuration = (PeriodDatabaseExtensionSchemaConfiguration) o;
        return super.equals(configuration) && nameSpace.equals(configuration.nameSpace);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + nameSpace.hashCode();
    }
}
