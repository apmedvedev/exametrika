/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core.config.schema;

import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link SpaceSchemaConfiguration} represents an abstract configuration of schema of space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class SpaceSchemaConfiguration extends SchemaConfiguration {
    public SpaceSchemaConfiguration(String name, String alias, String description) {
        super(name, alias, description);
    }

    public abstract ISpaceSchema createSchema(IDatabaseContext context, int version);

    public void freeze() {
    }

    public abstract void orderNodes(SpaceSchemaConfiguration oldSchema);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SpaceSchemaConfiguration))
            return false;

        SpaceSchemaConfiguration configuration = (SpaceSchemaConfiguration) o;
        return super.equals(configuration);
    }

    public boolean equalsStructured(SpaceSchemaConfiguration newSchema) {
        return getName().equals(newSchema.getName());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
