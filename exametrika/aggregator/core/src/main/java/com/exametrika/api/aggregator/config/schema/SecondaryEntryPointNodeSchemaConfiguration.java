/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import java.util.List;

import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration.Kind;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.nodes.SecondaryEntryPointNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;

/**
 * The {@link SecondaryEntryPointNodeSchemaConfiguration} represents a configuration of schema of secondary entry point node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class SecondaryEntryPointNodeSchemaConfiguration extends EntryPointNodeSchemaConfiguration {
    public SecondaryEntryPointNodeSchemaConfiguration(String name, String alias, String description,
                                                      IndexedLocationFieldSchemaConfiguration primaryField, AggregationComponentTypeSchemaConfiguration componentType,
                                                      FieldSchemaConfiguration aggregationField, List<? extends FieldSchemaConfiguration> fields) {
        super(name, alias, description, primaryField, componentType, aggregationField, fields);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new SecondaryEntryPointNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SecondaryEntryPointNodeSchemaConfiguration))
            return false;

        SecondaryEntryPointNodeSchemaConfiguration configuration = (SecondaryEntryPointNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof SecondaryEntryPointNodeSchemaConfiguration))
            return false;

        SecondaryEntryPointNodeSchemaConfiguration configuration = (SecondaryEntryPointNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected void check(AggregationComponentTypeSchemaConfiguration componentType) {
        Assert.isTrue(componentType.getKind() == Kind.SECONDARY_ENTRY_POINT);
    }

    @Override
    protected Class getNodeClass() {
        return SecondaryEntryPointNode.class;
    }
}
