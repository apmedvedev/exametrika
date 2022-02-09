/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import java.util.List;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.impl.component.nodes.ComponentRootNode;
import com.exametrika.impl.component.schema.ComponentRootNodeSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;

/**
 * The {@link ComponentRootNodeSchemaConfiguration} represents a configuration of schema of root node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentRootNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
    public ComponentRootNodeSchemaConfiguration(String name, String alias, String description,
                                                List<? extends FieldSchemaConfiguration> fields) {
        super(name, alias, description, fields, null);
    }

    @Override
    public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
        return new ComponentRootNodeSchema(this, index, fields, documentSchema);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new ComponentRootNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ComponentRootNodeSchemaConfiguration))
            return false;

        ComponentRootNodeSchemaConfiguration configuration = (ComponentRootNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof ComponentRootNodeSchemaConfiguration))
            return false;

        ComponentRootNodeSchemaConfiguration configuration = (ComponentRootNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected Class getNodeClass() {
        return ComponentRootNode.class;
    }
}
