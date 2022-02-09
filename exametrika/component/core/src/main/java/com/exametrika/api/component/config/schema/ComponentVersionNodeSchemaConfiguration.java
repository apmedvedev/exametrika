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
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.nodes.ComponentVersionNode;
import com.exametrika.impl.component.schema.ComponentVersionNodeSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;

/**
 * The {@link ComponentVersionNodeSchemaConfiguration} represents a configuration of schema of component version node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ComponentVersionNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
    private ComponentSchemaConfiguration component;

    public ComponentVersionNodeSchemaConfiguration(String name, String alias, String description,
                                                   List<? extends FieldSchemaConfiguration> fields, ComponentSchemaConfiguration component) {
        super(name, alias, description, fields, null);

        Assert.notNull(component);

        this.component = component;
    }

    public ComponentSchemaConfiguration getComponent() {
        return component;
    }

    @Override
    public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
        return new ComponentVersionNodeSchema(this, index, fields, documentSchema);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new ComponentVersionNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ComponentVersionNodeSchemaConfiguration))
            return false;

        ComponentVersionNodeSchemaConfiguration configuration = (ComponentVersionNodeSchemaConfiguration) o;
        return super.equals(configuration) && component.equals(configuration.component);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof ComponentVersionNodeSchemaConfiguration))
            return false;

        ComponentVersionNodeSchemaConfiguration configuration = (ComponentVersionNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && component.equalsStructured(configuration.component);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(component);
    }

    @Override
    protected Class getNodeClass() {
        return ComponentVersionNode.class;
    }
}
