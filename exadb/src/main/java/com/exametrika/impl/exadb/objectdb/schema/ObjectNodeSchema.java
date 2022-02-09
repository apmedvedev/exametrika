/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.util.List;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectNodeSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link ObjectNodeSchema} represents a schema of aggregation node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ObjectNodeSchema extends NodeSchema implements IObjectNodeSchema {
    public ObjectNodeSchema(ObjectNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                            IDocumentSchema fullTextSchema) {
        super(configuration, index, fields, fullTextSchema);
    }

    @Override
    public ObjectNodeSchemaConfiguration getConfiguration() {
        return (ObjectNodeSchemaConfiguration) super.getConfiguration();
    }
}
