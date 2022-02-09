/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import java.util.List;

import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.impl.aggregator.nodes.EntryPointNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;

/**
 * The {@link EntryPointNodeSchemaConfiguration} represents a configuration of schema of entry point node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class EntryPointNodeSchemaConfiguration extends StackNodeSchemaConfiguration {
    public EntryPointNodeSchemaConfiguration(String name, String alias, String description,
                                             IndexedLocationFieldSchemaConfiguration primaryField, AggregationComponentTypeSchemaConfiguration componentType,
                                             FieldSchemaConfiguration aggregationField, List<? extends FieldSchemaConfiguration> fields) {
        super(name, alias, description, primaryField, componentType, aggregationField, fields);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new EntryPointNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof EntryPointNodeSchemaConfiguration))
            return false;

        EntryPointNodeSchemaConfiguration configuration = (EntryPointNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof EntryPointNodeSchemaConfiguration))
            return false;

        EntryPointNodeSchemaConfiguration configuration = (EntryPointNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected Class getNodeClass() {
        return EntryPointNode.class;
    }
}
