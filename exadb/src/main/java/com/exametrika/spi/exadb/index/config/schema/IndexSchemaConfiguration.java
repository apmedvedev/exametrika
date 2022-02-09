/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.index.config.schema;

import java.util.Map;

import com.exametrika.api.exadb.index.IIndex;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;


/**
 * The {@link IndexSchemaConfiguration} is a configuration of index schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class IndexSchemaConfiguration extends SchemaConfiguration {
    public static final String SCHEMA = "com.exametrika.exadb.index-1.0";

    private final int pathIndex;
    private final Map<String, String> properties;

    public IndexSchemaConfiguration(String name, int pathIndex, Map<String, String> properties) {
        this(name, name, null, pathIndex, properties);
    }

    public IndexSchemaConfiguration(String name, String alias, String description, int pathIndex, Map<String, String> properties) {
        super(name, alias, description);

        Assert.notNull(properties);

        this.pathIndex = pathIndex;
        this.properties = Immutables.wrap(properties);
    }

    public int getPathIndex() {
        return pathIndex;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public abstract String getType();

    public abstract IIndex createIndex(String filePrefix, IIndexManager indexManager, IDatabaseContext context);

    public abstract IIndex openIndex(int id, String filePrefix, IIndexManager indexManager, IDatabaseContext context);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IndexSchemaConfiguration))
            return false;

        IndexSchemaConfiguration configuration = (IndexSchemaConfiguration) o;
        return super.equals(configuration) && pathIndex == configuration.pathIndex && properties.equals(configuration.properties);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(pathIndex, properties);
    }
}
