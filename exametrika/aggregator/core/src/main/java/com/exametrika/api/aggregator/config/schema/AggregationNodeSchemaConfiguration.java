/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.nodes.AggregationNode;
import com.exametrika.impl.aggregator.schema.AggregationNodeSchema;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;

/**
 * The {@link AggregationNodeSchemaConfiguration} represents a configuration of schema of aggregation node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AggregationNodeSchemaConfiguration extends PeriodNodeSchemaConfiguration {
    private final AggregationComponentTypeSchemaConfiguration componentType;
    private final FieldSchemaConfiguration aggregationField;

    public AggregationNodeSchemaConfiguration(String name, String alias, String description, IndexedLocationFieldSchemaConfiguration primaryField,
                                              AggregationComponentTypeSchemaConfiguration componentType, FieldSchemaConfiguration aggregationField,
                                              List<? extends FieldSchemaConfiguration> fields) {
        super(name, alias, description, primaryField, createFields(aggregationField, fields), null);

        Assert.notNull(componentType);
        Assert.isTrue(aggregationField instanceof LogAggregationFieldSchemaConfiguration || aggregationField instanceof PeriodAggregationFieldSchemaConfiguration);

        check(componentType);
        this.componentType = componentType;
        this.aggregationField = aggregationField;
    }

    public final AggregationComponentTypeSchemaConfiguration getComponentType() {
        return componentType;
    }

    public final FieldSchemaConfiguration getAggregationField() {
        return aggregationField;
    }

    public boolean isDerived() {
        return false;
    }

    @Override
    public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
        return new AggregationNodeSchema(this, index, fields, documentSchema);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new AggregationNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AggregationNodeSchemaConfiguration))
            return false;

        AggregationNodeSchemaConfiguration configuration = (AggregationNodeSchemaConfiguration) o;
        return super.equals(configuration) && componentType.equals(configuration.componentType);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof AggregationNodeSchemaConfiguration))
            return false;

        AggregationNodeSchemaConfiguration configuration = (AggregationNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && componentType.equalsStructured(configuration.componentType);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(componentType);
    }

    protected void check(AggregationComponentTypeSchemaConfiguration componentType) {
    }

    @Override
    protected Class getNodeClass() {
        return AggregationNode.class;
    }

    private static List<? extends FieldSchemaConfiguration> createFields(
            FieldSchemaConfiguration aggregationField, List<? extends FieldSchemaConfiguration> fields) {
        List<FieldSchemaConfiguration> list = new ArrayList<FieldSchemaConfiguration>(fields);
        list.add(aggregationField);
        return list;
    }
}
