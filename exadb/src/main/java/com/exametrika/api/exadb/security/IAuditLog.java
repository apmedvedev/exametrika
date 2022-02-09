/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security;

import com.exametrika.api.exadb.objectdb.fields.IStructuredBlobField;


/**
 * The {@link IAuditLog} represents an audit log.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IAuditLog extends IStructuredBlobField.IStructuredIterable<IAuditRecord> {
    /**
     * Clears audit log.
     */
    void clear();
}
