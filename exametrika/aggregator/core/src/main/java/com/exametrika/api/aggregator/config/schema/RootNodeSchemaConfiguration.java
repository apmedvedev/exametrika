/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import java.util.List;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.impl.aggregator.nodes.RootNode;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;

/**
 * The {@link RootNodeSchemaConfiguration} represents a configuration of schema of root node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class RootNodeSchemaConfiguration extends PeriodNodeSchemaConfiguration {
    public RootNodeSchemaConfiguration(String name, String alias, String description, IndexedLocationFieldSchemaConfiguration primaryField,
                                       List<? extends FieldSchemaConfiguration> fields) {
        super(name, alias, description, primaryField, fields, null);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new RootNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RootNodeSchemaConfiguration))
            return false;

        RootNodeSchemaConfiguration configuration = (RootNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof RootNodeSchemaConfiguration))
            return false;

        RootNodeSchemaConfiguration configuration = (RootNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected Class getNodeClass() {
        return RootNode.class;
    }
}
