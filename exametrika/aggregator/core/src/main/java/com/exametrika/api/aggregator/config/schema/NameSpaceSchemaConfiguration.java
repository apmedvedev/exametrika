/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;

/**
 * The {@link NameSpaceSchemaConfiguration} represents a configuration of schema of name space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NameSpaceSchemaConfiguration extends SchemaConfiguration {
    private final int nameSpacePathIndex;
    private final int nameIndexPathIndex;
    private final int maxNameSize;

    public NameSpaceSchemaConfiguration() {
        this("nameSpace", 0, 0, 256);
    }

    public NameSpaceSchemaConfiguration(String name, int nameSpacePathIndex, int nameIndexPathIndex, int maxNameSize) {
        this(name, name, null, nameSpacePathIndex, nameIndexPathIndex, maxNameSize);
    }

    public NameSpaceSchemaConfiguration(String name, String alias, String description, int nameSpacePathIndex,
                                        int nameIndexPathIndex, int maxNameSize) {
        super(name, alias, description);

        Assert.isTrue(maxNameSize <= Constants.PAGE_SIZE / 32);

        this.nameSpacePathIndex = nameSpacePathIndex;
        this.nameIndexPathIndex = nameIndexPathIndex;
        this.maxNameSize = maxNameSize;
    }

    public int getNameSpacePathIndex() {
        return nameSpacePathIndex;
    }

    public int getNameIndexPathIndex() {
        return nameIndexPathIndex;
    }

    public int getMaxNameSize() {
        return maxNameSize;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NameSpaceSchemaConfiguration))
            return false;

        NameSpaceSchemaConfiguration configuration = (NameSpaceSchemaConfiguration) o;
        return super.equals(configuration) && nameSpacePathIndex == configuration.nameSpacePathIndex &&
                nameIndexPathIndex == configuration.nameIndexPathIndex &&
                maxNameSize == configuration.maxNameSize;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(nameSpacePathIndex, nameIndexPathIndex, maxNameSize);
    }
}
