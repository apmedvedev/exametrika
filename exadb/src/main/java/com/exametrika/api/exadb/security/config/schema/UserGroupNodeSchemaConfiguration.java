/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security.config.schema;

import java.util.Arrays;
import java.util.List;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedStringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.JsonFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SerializableFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.impl.exadb.security.model.UserGroupNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link UserGroupNodeSchemaConfiguration} is a user group node schema configuration.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UserGroupNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
    public UserGroupNodeSchemaConfiguration() {
        super("UserGroup", createFields());
    }

    @Override
    public INodeObject createNode(INode node) {
        return new UserGroupNode(node);
    }

    @Override
    protected Class getNodeClass() {
        return UserGroupNode.class;
    }

    private static List<? extends FieldSchemaConfiguration> createFields() {
        return Arrays.asList(
                new StringFieldSchemaConfiguration("name", 256),
                new StringFieldSchemaConfiguration("description", 256),
                new JsonFieldSchemaConfiguration("metadata"),
                new ReferenceFieldSchemaConfiguration("roles", null),
                new SerializableFieldSchemaConfiguration("labels"),
                new IndexedStringFieldSchemaConfiguration("id", true, 256),
                new SingleReferenceFieldSchemaConfiguration("parent", null),
                new ReferenceFieldSchemaConfiguration("children", null),
                new ReferenceFieldSchemaConfiguration("users", null));
    }
}