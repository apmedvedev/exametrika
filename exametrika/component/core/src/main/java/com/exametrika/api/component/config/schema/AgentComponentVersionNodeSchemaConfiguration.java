/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import java.util.List;

import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.impl.component.nodes.AgentComponentVersionNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;

/**
 * The {@link AgentComponentVersionNodeSchemaConfiguration} represents a configuration of schema of agent aggregation component version node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AgentComponentVersionNodeSchemaConfiguration extends HealthComponentVersionNodeSchemaConfiguration {
    public AgentComponentVersionNodeSchemaConfiguration(String name, String alias, String description,
                                                        List<? extends FieldSchemaConfiguration> fields, ComponentSchemaConfiguration component) {
        super(name, alias, description, fields, component);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new AgentComponentVersionNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AgentComponentVersionNodeSchemaConfiguration))
            return false;

        AgentComponentVersionNodeSchemaConfiguration configuration = (AgentComponentVersionNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected Class getNodeClass() {
        return AgentComponentVersionNode.class;
    }
}
