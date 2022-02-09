/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.config.schema;

import java.util.List;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;
import com.exametrika.impl.exadb.objectdb.schema.ObjectNodeSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;

/**
 * The {@link ObjectNodeSchemaConfiguration} represents a configuration of schema of period node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ObjectNodeSchemaConfiguration extends NodeSchemaConfiguration {
    public ObjectNodeSchemaConfiguration(String name, List<? extends FieldSchemaConfiguration> fields) {
        this(name, name, null, fields, null);
    }

    public ObjectNodeSchemaConfiguration(String name, List<? extends FieldSchemaConfiguration> fields, String documentType) {
        this(name, name, null, fields, documentType);
    }

    public ObjectNodeSchemaConfiguration(String name, String alias, String description,
                                         List<? extends FieldSchemaConfiguration> fields, String documentType) {
        super(name, alias, description, fields, documentType);
    }

    @Override
    public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
        return new ObjectNodeSchema(this, index, fields, documentSchema);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new ObjectNodeObject(node);
    }

    @Override
    protected Class getNodeClass() {
        return ObjectNodeObject.class;
    }
}
