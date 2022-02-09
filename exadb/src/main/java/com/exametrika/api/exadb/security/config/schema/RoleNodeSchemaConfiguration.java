/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security.config.schema;

import java.util.Arrays;
import java.util.List;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.IndexType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedStringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.JsonFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.impl.exadb.security.model.RoleNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link RoleNodeSchemaConfiguration} is a role node schema configuration.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class RoleNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
    public RoleNodeSchemaConfiguration() {
        super("Role", createFields());
    }

    @Override
    public INodeObject createNode(INode node) {
        return new RoleNode(node);
    }

    @Override
    protected Class getNodeClass() {
        return RoleNode.class;
    }

    private static List<? extends FieldSchemaConfiguration> createFields() {
        return Arrays.asList(new IndexedStringFieldSchemaConfiguration("name", "name", null, true, true, 0, 64, null, null, null,
                        0, IndexType.BTREE, false, false, true, true, false, null, false, false, null, null),
                new SingleReferenceFieldSchemaConfiguration("subject", null),
                new JsonFieldSchemaConfiguration("metadata"));
    }
}