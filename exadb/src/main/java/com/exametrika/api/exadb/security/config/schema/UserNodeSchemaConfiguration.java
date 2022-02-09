/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security.config.schema;

import java.util.Arrays;
import java.util.List;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedStringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.JsonFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SerializableFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.impl.exadb.security.model.UserNode;
import com.exametrika.impl.exadb.security.schema.UserNodeSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link UserNodeSchemaConfiguration} is a user node schema configuration.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UserNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
    public UserNodeSchemaConfiguration() {
        super("User", createFields());
    }

    @Override
    public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
        return new UserNodeSchema(this, index, fields, documentSchema);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new UserNode(node);
    }

    @Override
    protected Class getNodeClass() {
        return UserNode.class;
    }

    private static List<? extends FieldSchemaConfiguration> createFields() {
        return Arrays.asList(new IndexedStringFieldSchemaConfiguration("name", false, 256),
                new StringFieldSchemaConfiguration("description", 256),
                new JsonFieldSchemaConfiguration("metadata"),
                new ReferenceFieldSchemaConfiguration("roles", null),
                new SerializableFieldSchemaConfiguration("labels"),
                new ReferenceFieldSchemaConfiguration("groups", null),
                new SerializableFieldSchemaConfiguration("credentials"));
    }
}