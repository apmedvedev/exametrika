/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.util.Map;

import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.schema.SchemaObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;

/**
 * The {@link FieldSchema} is an abstract field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class FieldSchema extends SchemaObject implements IFieldSchema {
    protected final FieldSchemaConfiguration configuration;
    protected INodeSchema parent;
    protected final int index;
    protected final int offset;

    public FieldSchema(FieldSchemaConfiguration configuration, int index, int offset) {
        super(TYPE);

        Assert.notNull(configuration);

        this.configuration = configuration;
        this.index = index;
        this.offset = offset;
    }

    public void setParent(INodeSchema parent, Map<String, ISchemaObject> schemaObjects) {
        Assert.notNull(parent);

        this.parent = parent;
        super.setParent(parent, schemaObjects);
    }

    @Override
    public FieldSchemaConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public final int getIndex() {
        return index;
    }

    @Override
    public final int getOffset() {
        return offset;
    }

    @Override
    public int getIndexTotalIndex() {
        return -1;
    }

    @Override
    public INodeSchema getParent() {
        return parent;
    }

    @Override
    public void resolveDependencies() {
    }
}
