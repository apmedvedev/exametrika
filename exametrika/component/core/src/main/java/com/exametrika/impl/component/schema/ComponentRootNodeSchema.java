/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import java.util.List;

import com.exametrika.api.component.config.schema.ComponentRootNodeSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.impl.exadb.objectdb.schema.ObjectNodeSchema;


/**
 * The {@link ComponentRootNodeSchema} represents a schema of component node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ComponentRootNodeSchema extends ObjectNodeSchema {
    private boolean deletedComponentFiltered = true;

    public ComponentRootNodeSchema(ComponentRootNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                                   IDocumentSchema fullTextSchema) {
        super(configuration, index, fields, fullTextSchema);
    }

    @Override
    public ComponentRootNodeSchemaConfiguration getConfiguration() {
        return (ComponentRootNodeSchemaConfiguration) super.getConfiguration();
    }

    public boolean isDeletedComponentFiltered() {
        return deletedComponentFiltered;
    }

    public void setDeletedComponentFiltered(boolean value) {
        deletedComponentFiltered = value;
    }
}
