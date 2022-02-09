/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security;


/**
 * The {@link IPermission} represents a runtime representation of specific permission.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IPermission {
    /**
     * Returns permission name.
     *
     * @return permission name
     */
    String getName();

    /**
     * Returns permission index.
     *
     * @return permission index
     */
    int getIndex();

    /**
     * Is access from current user allowed to specified object.
     *
     * @param object object to check
     * @return true if access from current user is allowed to specified object
     */
    boolean isAccessAllowed(Object object);

    /**
     * Checks access from current user to specified object.
     *
     * @param object object to check
     * @throws IllegalStateException if access from current user to specified object is denied
     */
    void check(Object object);

    /**
     * Checks access from current user to specified object. Allows to organize authorization checks on secured api boundaries only,
     * effectively disabling all checks on inner calls. Each call to this method must be paired with {@link #endCheck()}.
     *
     * @param object object to check
     * @throws IllegalStateException if access from current user to specified object is denied
     */
    void beginCheck(Object object);

    /**
     * Marks end of call of some secured method.
     */
    void endCheck();
}
