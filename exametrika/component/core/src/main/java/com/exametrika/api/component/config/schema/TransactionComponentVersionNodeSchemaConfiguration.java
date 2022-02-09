/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import java.util.List;

import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.impl.component.nodes.TransactionComponentVersionNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;

/**
 * The {@link TransactionComponentVersionNodeSchemaConfiguration} represents a configuration of schema of transaction aggregation component version node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class TransactionComponentVersionNodeSchemaConfiguration extends HealthComponentVersionNodeSchemaConfiguration {
    public TransactionComponentVersionNodeSchemaConfiguration(String name, String alias, String description,
                                                              List<? extends FieldSchemaConfiguration> fields, ComponentSchemaConfiguration component) {
        super(name, alias, description, fields, component);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new TransactionComponentVersionNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TransactionComponentVersionNodeSchemaConfiguration))
            return false;

        TransactionComponentVersionNodeSchemaConfiguration configuration = (TransactionComponentVersionNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof TransactionComponentVersionNodeSchemaConfiguration))
            return false;

        TransactionComponentVersionNodeSchemaConfiguration configuration = (TransactionComponentVersionNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected Class getNodeClass() {
        return TransactionComponentVersionNode.class;
    }
}
