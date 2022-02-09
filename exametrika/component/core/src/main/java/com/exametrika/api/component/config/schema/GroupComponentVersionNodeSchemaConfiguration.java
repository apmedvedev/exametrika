/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import java.util.List;

import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.impl.component.nodes.GroupComponentVersionNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;

/**
 * The {@link GroupComponentVersionNodeSchemaConfiguration} represents a configuration of schema of group component version node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class GroupComponentVersionNodeSchemaConfiguration extends HealthComponentVersionNodeSchemaConfiguration {
    public GroupComponentVersionNodeSchemaConfiguration(String name, String alias, String description,
                                                        List<? extends FieldSchemaConfiguration> fields, ComponentSchemaConfiguration component) {
        super(name, alias, description, fields, component);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new GroupComponentVersionNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof GroupComponentVersionNodeSchemaConfiguration))
            return false;

        GroupComponentVersionNodeSchemaConfiguration configuration = (GroupComponentVersionNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected Class getNodeClass() {
        return GroupComponentVersionNode.class;
    }
}
