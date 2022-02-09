/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import java.util.List;

import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.nodes.StackNode;
import com.exametrika.impl.aggregator.schema.StackNodeSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;

/**
 * The {@link StackNodeSchemaConfiguration} represents a configuration of schema of stack node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StackNodeSchemaConfiguration extends AggregationNodeSchemaConfiguration {
    public StackNodeSchemaConfiguration(String name, String alias, String description, IndexedLocationFieldSchemaConfiguration primaryField,
                                        AggregationComponentTypeSchemaConfiguration componentType, FieldSchemaConfiguration aggregationField,
                                        List<? extends FieldSchemaConfiguration> fields) {
        super(name, alias, description, primaryField, componentType, aggregationField, fields);
    }

    @Override
    public boolean isStack() {
        return true;
    }

    @Override
    public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
        return new StackNodeSchema(this, index, fields, documentSchema);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new StackNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackNodeSchemaConfiguration))
            return false;

        StackNodeSchemaConfiguration configuration = (StackNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof StackNodeSchemaConfiguration))
            return false;

        StackNodeSchemaConfiguration configuration = (StackNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected void check(AggregationComponentTypeSchemaConfiguration componentType) {
        Assert.isTrue(componentType instanceof StackSchemaConfiguration);
    }

    @Override
    protected Class getNodeClass() {
        return StackNode.class;
    }
}
