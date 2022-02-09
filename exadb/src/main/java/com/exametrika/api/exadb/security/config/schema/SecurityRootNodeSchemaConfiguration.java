/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security.config.schema;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration.PageType;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.impl.exadb.security.model.SecurityRootNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link SecurityRootNodeSchemaConfiguration} is a security root node schema configuration.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SecurityRootNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
    public SecurityRootNodeSchemaConfiguration() {
        super("root", createFields());
    }

    @Override
    public INodeObject createNode(INode node) {
        return new SecurityRootNode(node);
    }

    @Override
    protected Class getNodeClass() {
        return SecurityRootNode.class;
    }

    private static List<? extends FieldSchemaConfiguration> createFields() {
        return Arrays.asList(
                new SingleReferenceFieldSchemaConfiguration("rootGroup", null),
                new BlobStoreFieldSchemaConfiguration("blobStore", "blobStore", null, 0, Long.MAX_VALUE, null,
                        PageType.NORMAL, false, Collections.<String, String>emptyMap(), false),
                new AuditLogFieldSchemaConfiguration("auditLog", "auditLog", null, "blobStore"));
    }
}