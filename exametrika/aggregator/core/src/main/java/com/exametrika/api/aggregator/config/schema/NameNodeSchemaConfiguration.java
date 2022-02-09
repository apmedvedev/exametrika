/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import java.util.List;

import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration.Kind;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.nodes.NameNode;
import com.exametrika.impl.aggregator.schema.NameNodeSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;

/**
 * The {@link NameNodeSchemaConfiguration} represents a configuration of schema of name node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class NameNodeSchemaConfiguration extends AggregationNodeSchemaConfiguration {
    public NameNodeSchemaConfiguration(String name, String alias, String description, IndexedLocationFieldSchemaConfiguration primaryField,
                                       AggregationComponentTypeSchemaConfiguration componentType, FieldSchemaConfiguration aggregationField,
                                       List<? extends FieldSchemaConfiguration> fields) {
        super(name, alias, description, primaryField, componentType, aggregationField, fields);
    }

    @Override
    public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
        return new NameNodeSchema(this, index, fields, documentSchema);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new NameNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NameNodeSchemaConfiguration))
            return false;

        NameNodeSchemaConfiguration configuration = (NameNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof NameNodeSchemaConfiguration))
            return false;

        NameNodeSchemaConfiguration configuration = (NameNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected void check(AggregationComponentTypeSchemaConfiguration componentType) {
        Assert.isTrue(componentType.getKind() == Kind.NAME);
    }

    @Override
    protected Class getNodeClass() {
        return NameNode.class;
    }
}
