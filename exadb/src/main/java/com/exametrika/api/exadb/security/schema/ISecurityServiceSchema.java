/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security.schema;

import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.api.exadb.security.IPermission;


/**
 * The {@link ISecurityServiceSchema} represents a schema for security service.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISecurityServiceSchema extends IDomainServiceSchema {
    /**
     * Creates permission.
     *
     * @param name      permission name
     * @param auditable if true permission is auditable
     * @return permission
     */
    IPermission createPermission(String name, boolean auditable);
}
