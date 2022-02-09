/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.exa.server.config.schema;

import java.util.List;

import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.component.config.schema.HealthComponentVersionNodeSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.impl.metrics.exa.server.nodes.ExaServerComponentVersionNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;

/**
 * The {@link ExaServerComponentVersionNodeSchemaConfiguration} represents a configuration of schema of exa server aggregation component version node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExaServerComponentVersionNodeSchemaConfiguration extends HealthComponentVersionNodeSchemaConfiguration {
    public ExaServerComponentVersionNodeSchemaConfiguration(String name, String alias, String description,
                                                            List<? extends FieldSchemaConfiguration> fields, ComponentSchemaConfiguration component) {
        super(name, alias, description, fields, component);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new ExaServerComponentVersionNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExaServerComponentVersionNodeSchemaConfiguration))
            return false;

        ExaServerComponentVersionNodeSchemaConfiguration configuration = (ExaServerComponentVersionNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected Class getNodeClass() {
        return ExaServerComponentVersionNode.class;
    }
}
