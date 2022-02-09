/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.schema;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.config.schema.IndexedLocationFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.impl.aggregator.nodes.PeriodNodeObject;
import com.exametrika.impl.aggregator.schema.PeriodNodeSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;

/**
 * The {@link PeriodNodeSchemaConfiguration} represents a configuration of schema of period node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class PeriodNodeSchemaConfiguration extends NodeSchemaConfiguration {
    public PeriodNodeSchemaConfiguration(String name, IndexedLocationFieldSchemaConfiguration primaryField,
                                         List<? extends FieldSchemaConfiguration> fields, String documentType) {
        this(name, name, null, primaryField, fields, documentType);
    }

    public PeriodNodeSchemaConfiguration(String name, String alias, String description,
                                         IndexedLocationFieldSchemaConfiguration primaryField,
                                         List<? extends FieldSchemaConfiguration> fields, String documentType) {
        super(name, alias, description, createFields(primaryField, fields), documentType);
    }

    @Override
    public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
        return new PeriodNodeSchema(this, index, fields, documentSchema);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new PeriodNodeObject(node);
    }

    public boolean isStack() {
        return false;
    }

    @Override
    protected Class getNodeClass() {
        return PeriodNodeObject.class;
    }

    private static List<? extends FieldSchemaConfiguration> createFields(IndexedLocationFieldSchemaConfiguration primaryField,
                                                                         List<? extends FieldSchemaConfiguration> fields) {
        List<FieldSchemaConfiguration> list = new ArrayList<FieldSchemaConfiguration>();
        list.add(primaryField);
        list.addAll(fields);
        return list;
    }
}
