/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import java.util.List;

import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.impl.component.nodes.GroupComponentNode;
import com.exametrika.impl.component.schema.GroupComponentNodeSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;

/**
 * The {@link GroupComponentNodeSchemaConfiguration} represents a configuration of schema of group component node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class GroupComponentNodeSchemaConfiguration extends HealthComponentNodeSchemaConfiguration {
    public GroupComponentNodeSchemaConfiguration(String name, String alias, String description,
                                                 List<? extends FieldSchemaConfiguration> fields, ComponentSchemaConfiguration component) {
        super(name, alias, description, fields, component);
    }

    @Override
    public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
        return new GroupComponentNodeSchema(this, index, fields, documentSchema);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new GroupComponentNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof GroupComponentNodeSchemaConfiguration))
            return false;

        GroupComponentNodeSchemaConfiguration configuration = (GroupComponentNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof GroupComponentNodeSchemaConfiguration))
            return false;

        GroupComponentNodeSchemaConfiguration configuration = (GroupComponentNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected Class getNodeClass() {
        return GroupComponentNode.class;
    }
}
