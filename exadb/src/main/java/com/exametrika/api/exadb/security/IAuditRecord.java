/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security;


/**
 * The {@link IAuditRecord} represents an audit record.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IAuditRecord {
    /**
     * Returns user.
     *
     * @return user
     */
    String getUser();

    /**
     * Returns permission.
     *
     * @return permission
     */
    String getPermission();

    /**
     * Returns object.
     *
     * @return object or null if object is not present
     */
    String getObject();

    /**
     * Returns time
     *
     * @return time
     */
    long getTime();

    /**
     * Is permission check succeeded?
     *
     * @return true if permission check is succeeded
     */
    boolean isSucceeded();
}
