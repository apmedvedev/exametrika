/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.security;

import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.api.exadb.security.ISecurityService;
import com.exametrika.api.exadb.security.schema.ISecurityServiceSchema;


/**
 * The {@link Permissions} represents a utility class for permission creation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Permissions {
    private static final String ID = IDomainServiceSchema.TYPE + ":" + ISecurityService.NAME;

    public static IPermission permission(ISchemaObject schema, String name, boolean auditable) {
        IDatabaseSchema root = schema.getRoot();
        ISecurityServiceSchema securityServiceSchema = root.findSchemaById(ID);
        return securityServiceSchema.createPermission(name, auditable);
    }

    private Permissions() {
    }
}
