/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import java.util.List;

import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration.Kind;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.nodes.StackNameNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;

/**
 * The {@link StackNameNodeSchemaConfiguration} represents a configuration of schema of stack name node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StackNameNodeSchemaConfiguration extends NameNodeSchemaConfiguration {
    public StackNameNodeSchemaConfiguration(String name, String alias, String description, IndexedLocationFieldSchemaConfiguration primaryField,
                                            AggregationComponentTypeSchemaConfiguration componentType, FieldSchemaConfiguration aggregationField,
                                            List<? extends FieldSchemaConfiguration> fields) {
        super(name, alias, description, primaryField, componentType, aggregationField, fields);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new StackNameNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackNameNodeSchemaConfiguration))
            return false;

        StackNameNodeSchemaConfiguration configuration = (StackNameNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof StackNameNodeSchemaConfiguration))
            return false;

        StackNameNodeSchemaConfiguration configuration = (StackNameNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected void check(AggregationComponentTypeSchemaConfiguration componentType) {
        Assert.isTrue(componentType.getKind() == Kind.STACK_NAME);
    }

    @Override
    protected Class getNodeClass() {
        return StackNameNode.class;
    }
}
